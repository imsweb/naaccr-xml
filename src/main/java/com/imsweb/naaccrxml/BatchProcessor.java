/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package com.imsweb.naaccrxml;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.imsweb.naaccrxml.entity.Patient;

/**
 * This class can be called on the command line to process an entire folder of files.
 * <br/>
 * Usage: java -cp naaccr-xml-x.x-all.jar BatchProcessor options.properties
 */
public final class BatchProcessor {

    // full path to the folder containing the input files (required)
    private static final String _OPTION_INPUT_FOLDER = "input.folder";

    // file name inclusion regex (optional)
    private static final String _OPTION_INPUT_REGEX_INCLUDE = "input.regex-include";

    // file name exclusion regex (optional)
    private static final String _OPTION_INPUT_REGEX_EXCLUDE = "input.regex-exclude";

    // processing mode: flat-to-xml or xml-to-flat (required)
    private static final String _OPTION_PROCESSING_MODE = "processing.mode";

    // the list of error codes (comma separated) to process (optional, if not provided, all codes are processed)
    private static final String _OPTION_PROCESSING_ERROR_CODES = "processing.error-codes";

    // the number of threads to use (optional, defaults to Min(num-processors + 1, 5))
    private static final String _OPTION_PROCESSING_NUM_THREADS = "processing.num-threads";

    // compression for the created data files; gz, xz, none, as-input (optional, defaults to as-input)
    private static final String _OPTION_PROCESSING_COMPRESSION = "processing.compression";

    // full path to the folder where the files should be created (required)
    private static final String _OPTION_OUTPUT_FOLDER = "output.folder";

    // whether or not the created files should be auto-deleted: true or false (defaults to false)
    private static final String _OPTION_OUTPUT_CLEAN_CREATED_FILES = "output.clean-created-files";

    // whether or not a report file (report.txt) should be created in the output folder: true or false (defaults to false)
    private static final String _OPTION_OUTPUT_CREATE_REPORT = "output.create-report";

    // the report name (optional, defaults to report.txt)
    private static final String _OPTION_OUTPUT_REPORT_NAME = "output.report-name";

    // whether or not the file names should be de-identified (defaults to false)
    private static final String _OPTION_OUTPUT_DEIDENTIFY_FILES = "output.de-identify-files";

    /**
     * Main method, entry point.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws IOException, InterruptedException {

        // read the options
        Properties opt = readOptions(args);
        if (opt == null)
            throw new IllegalStateException("Unable to find options file path, it must be provided as an argument to the call.");

        // validate the options
        if (opt.getProperty(_OPTION_INPUT_FOLDER) == null || opt.getProperty(_OPTION_INPUT_FOLDER).isEmpty())
            throw new IllegalStateException("Option " + _OPTION_INPUT_FOLDER + " is required.");
        File inputDir = new File(opt.getProperty(_OPTION_INPUT_FOLDER));
        if (!inputDir.exists())
            throw new IllegalStateException("Invalid input folder.");
        Pattern incRegex = opt.getProperty(_OPTION_INPUT_REGEX_INCLUDE) == null ? null : Pattern.compile(opt.getProperty(_OPTION_INPUT_REGEX_INCLUDE));
        Pattern excRegex = opt.getProperty(_OPTION_INPUT_REGEX_EXCLUDE) == null ? null : Pattern.compile(opt.getProperty(_OPTION_INPUT_REGEX_EXCLUDE));
        String mode = opt.getProperty(_OPTION_PROCESSING_MODE);
        if (mode == null)
            throw new IllegalStateException("Option " + _OPTION_PROCESSING_MODE + " is required.");
        if (!"flat-to-xml".equals(mode) && !"xml-to-flat".equals(mode))
            throw new IllegalStateException("Invalid mode (must be flat-to-xml or xml-to-flat).");
        String rawErrorCodes = opt.getProperty(_OPTION_PROCESSING_ERROR_CODES);
        List<String> errorCodes = null;
        if (rawErrorCodes != null && !rawErrorCodes.isEmpty()) {
            errorCodes = new ArrayList<>();
            for (String s : StringUtils.split(rawErrorCodes, ','))
                errorCodes.add(s.trim());
        }
        int numThreads = Math.min(Runtime.getRuntime().availableProcessors() + 1, 5);
        if (opt.getProperty(_OPTION_PROCESSING_NUM_THREADS) != null && !opt.getProperty(_OPTION_PROCESSING_NUM_THREADS).isEmpty())
            numThreads = Integer.parseInt(opt.getProperty(_OPTION_PROCESSING_NUM_THREADS));
        if (opt.getProperty(_OPTION_OUTPUT_FOLDER) == null || opt.getProperty(_OPTION_OUTPUT_FOLDER).isEmpty())
            throw new IllegalStateException("Option " + _OPTION_OUTPUT_FOLDER + " is required.");
        String compression = opt.getProperty(_OPTION_PROCESSING_COMPRESSION);
        if (compression != null && !compression.equals("gz") && !compression.equals("xz") && !compression.equals("none") && !compression.equals("as-input"))
            throw new IllegalStateException("Invalid compression (must be gz, xz, none, or as-input).");
        File outputDir = new File(opt.getProperty(_OPTION_OUTPUT_FOLDER));
        if (!outputDir.exists())
            throw new IllegalStateException("Invalid outupt folder.");
        boolean cleanCreatedFiles = opt.getProperty(_OPTION_OUTPUT_CLEAN_CREATED_FILES) != null && Boolean.parseBoolean(opt.getProperty(_OPTION_OUTPUT_CLEAN_CREATED_FILES));
        boolean createReport = opt.getProperty(_OPTION_OUTPUT_CREATE_REPORT) != null && Boolean.parseBoolean(opt.getProperty(_OPTION_OUTPUT_CREATE_REPORT));
        String reportName = opt.getProperty(_OPTION_OUTPUT_REPORT_NAME) == null ? "report.txt" : opt.getProperty(_OPTION_OUTPUT_REPORT_NAME);
        boolean deidentify = opt.getProperty(_OPTION_OUTPUT_DEIDENTIFY_FILES) != null && Boolean.parseBoolean(opt.getProperty(_OPTION_OUTPUT_DEIDENTIFY_FILES));

        // gather the files to process
        List<File> toProcess = new ArrayList<>();
        File[] files = inputDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    continue;
                boolean add = true;
                if (incRegex != null || excRegex != null) {
                    if (incRegex != null && !incRegex.matcher(file.getName()).matches())
                        add = false;
                    if (excRegex != null && excRegex.matcher(file.getName()).matches())
                        add = false;
                }
                if (add)
                    toProcess.add(file);
            }
        }

        // we will report the information in this collection
        Map<String, List<String>> reportData = new TreeMap<>();
        Map<String, AtomicInteger> globalCounts = new HashMap<>();
        Map<String, Set<String>> globalDetails = new HashMap<>();
        AtomicInteger globalTumorCount = new AtomicInteger();

        // create the work
        long start = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (File inputFile : toProcess) {
            String outputFilename = invertFilename(inputFile, compression);
            @SuppressWarnings("ConstantConditions")
            File outputFile = new File(outputDir, outputFilename);
            if (inputFile.equals(outputFile))
                throw new IllegalStateException("Was about to write output file into the input file, this can't be good!");
            if (cleanCreatedFiles)
                outputFile.deleteOnExit();
            List<String> data = new ArrayList<>();
            reportData.put(inputFile.getName(), data);
            executor.execute(new FileProcessor(inputFile, outputFile, data, cleanCreatedFiles, "flat-to-xml".equals(mode), globalCounts, globalDetails, globalTumorCount, errorCodes));
        }
        executor.shutdown();

        // wait for the work to be completed
        executor.awaitTermination(1, TimeUnit.DAYS);

        // write the report
        if (createReport) {
            try (Writer reportWriter = new OutputStreamWriter(Files.newOutputStream(new File(outputDir, reportName).toPath()), StandardCharsets.UTF_8);) {

                reportWriter.write("Report created on " + new Date() + "\n\n");
                reportWriter.write("total number of files: " + formatNumber(toProcess.size()) + "\n");
                reportWriter.write("total processing time: " + formatTime(System.currentTimeMillis() - start) + "\n");
                reportWriter.write("total number of processed tumors: " + formatNumber(globalTumorCount.get()) + "\n");
                reportWriter.write("combined warnings:\n");

                int globalCount = 0;
                for (String code : NaaccrErrorUtils.getAllValidationErrors().keySet()) {
                    if (errorCodes != null && !errorCodes.contains(code))
                        continue;
                    int count = globalCounts.containsKey(code) ? globalCounts.get(code).get() : 0;
                    if (count > 0) {
                        reportWriter.write("      " + code + ": " + formatNumber(count) + " cases\n");
                        if (globalDetails.containsKey(code)) {
                            List<String> list = new ArrayList<>(globalDetails.get(code));
                            Collections.sort(list);
                            reportWriter.write("         involved item(s): " + list.size() + " " + list + "\n");
                        }
                    }
                    globalCount += count;
                }
                if (globalCount == 0)
                    reportWriter.write("      no warning found\n");

                for (Entry<String, List<String>> entry : reportData.entrySet()) {
                    reportWriter.write("\n\n");
                    reportWriter.write(deidentify ? "<de-identified file name>" : entry.getKey());
                    reportWriter.write("\n");
                    for (String line : entry.getValue()) {
                        reportWriter.write(line);
                        reportWriter.write("\n");
                    }
                }
            }
        }
    }

    private static Properties readOptions(String[] args) {
        Properties opt = null;
        if (args.length != 0) {
            File file = new File(args[0]);
            if (file.exists()) {
                try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
                    opt = new Properties();
                    opt.load(reader);
                }
                catch (IOException e) {
                    opt = null;
                }
            }
        }
        return opt;
    }

    private static String invertFilename(File file, String compression) {
        // first invert the filename
        String[] name = StringUtils.split(file.getName(), '.');
        if (name.length < 2)
            return null;
        String extension = name[name.length - 1];
        boolean compressed = false;
        if (extension.equalsIgnoreCase("gz")) {
            extension = name[name.length - 2];
            compressed = true;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < (compressed ? name.length - 2 : name.length - 1); i++)
            result.append(name[i]).append(".");
        result.append(extension.equalsIgnoreCase("xml") ? "txt" : "xml");
        if (compressed)
            result.append(".gz");

        // then update the extension using the requested compression
        String newName = result.toString();
        if ("gz".equals(compression)) {
            if (newName.endsWith(".xz"))
                newName = newName.replace(".xz", "");
            if (!newName.endsWith(".gz"))
                newName = newName + ".gz";
        }
        else if ("xz".equals(compression)) {
            if (newName.endsWith(".gz"))
                newName = newName.replace(".gz", "");
            if (!newName.endsWith(".xz"))
                newName = newName + ".xz";
        }
        else if ("none".equals(compression)) {
            if (newName.endsWith(".gz"))
                newName = newName.replace(".gz", "");
            else if (newName.endsWith(".xz"))
                newName = newName.replace(".xz", "");
        }

        return new File(file.getParentFile(), newName).getName();
    }

    @SuppressWarnings("java:S4042")
    private static final class FileProcessor implements Runnable {

        private final File _inputFile;
        private final File _outputFile;
        private final List<String> _reportData;
        private final boolean _deleteOutputFiles;
        private final boolean _flatToXml;
        private final Map<String, AtomicInteger> _globalCounts;
        private final Map<String, Set<String>> _globalDetails;
        private final AtomicInteger _globalTumorCount;
        private final List<String> _errorCodes;

        public FileProcessor(File inputFile, File outputFile, List<String> reportData, boolean deleteOutputFiles, boolean flatToXml, Map<String, AtomicInteger> globalCounts, Map<String, Set<String>> globalDetails, AtomicInteger globalTumorCount, List<String> errorCodes) {
            _inputFile = inputFile;
            _outputFile = outputFile;
            _reportData = reportData;
            _deleteOutputFiles = deleteOutputFiles;
            _flatToXml = flatToXml;
            _globalCounts = globalCounts;
            _globalDetails = globalDetails;
            _globalTumorCount = globalTumorCount;
            _errorCodes = errorCodes;
        }

        @Override
        public void run() {

            Map<String, AtomicInteger> warningCounts = new HashMap<>();
            Map<String, Set<String>> warningDetails = new HashMap<>();
            AtomicInteger tumorCount = new AtomicInteger();

            NaaccrOptions options = new NaaccrOptions();
            options.setReportLevelMismatch(true);
            NaaccrObserver observer = new FileObserver(warningCounts, warningDetails, tumorCount, _globalCounts, _globalDetails, _globalTumorCount);

            try {
                long start = System.currentTimeMillis();

                if (_flatToXml)
                    NaaccrXmlUtils.flatToXml(_inputFile, _outputFile, options, null, observer);
                else
                    NaaccrXmlUtils.xmlToFlat(_inputFile, _outputFile, options, null, observer);

                _reportData.add("   original size: " + formatFileSize(_inputFile.length()));
                _reportData.add("   created size: " + formatFileSize(_outputFile.length()));
                _reportData.add("   processing time: " + formatTime(System.currentTimeMillis() - start));
                _reportData.add("   number of processed tumors: " + formatNumber(tumorCount.get()));
                _reportData.add("   warnings:");

                int globalCount = 0;
                for (String code : NaaccrErrorUtils.getAllValidationErrors().keySet()) {
                    if (_errorCodes != null && !_errorCodes.contains(code))
                        continue;
                    int count = warningCounts.containsKey(code) ? warningCounts.get(code).get() : 0;
                    if (count > 0) {
                        _reportData.add("      " + code + ": " + formatNumber(count) + " cases");
                        if (warningDetails.containsKey(code)) {
                            List<String> list = new ArrayList<>(warningDetails.get(code));
                            Collections.sort(list);
                            _reportData.add("         involved item(s): " + list.size() + " " + list);
                        }
                    }
                    globalCount += count;
                }
                if (globalCount == 0)
                    _reportData.add("      no warning found");
            }
            catch (NaaccrIOException e) {
                _reportData.add("   processing error: " + e.getMessage());
            }

            if (_deleteOutputFiles)
                if (!_outputFile.delete())
                    _reportData.add("Unable to delete " + _outputFile.getPath());
        }
    }

    private static final class FileObserver implements NaaccrObserver {

        private final Map<String, AtomicInteger> _warningCounts;
        private final Map<String, AtomicInteger> _globalCounts;
        private final Map<String, Set<String>> _warningDetails;
        private final Map<String, Set<String>> _globalDetails;
        private final AtomicInteger _tumorCount;
        private final AtomicInteger _globalTumorCount;

        public FileObserver(Map<String, AtomicInteger> warningCounts, Map<String, Set<String>> warningDetails, AtomicInteger tumorCount, Map<String, AtomicInteger> globalCounts, Map<String, Set<String>> globalDetails, AtomicInteger globalTumorCount) {
            _warningCounts = warningCounts;
            _warningDetails = warningDetails;
            _tumorCount = tumorCount;
            _globalCounts = globalCounts;
            _globalDetails = globalDetails;
            _globalTumorCount = globalTumorCount;
        }

        @Override
        public void patientRead(Patient patient) {
            handlePatient(patient);
        }

        @Override
        public void patientWritten(Patient patient) {
            handlePatient(patient);
            _tumorCount.addAndGet(patient.getTumors().size());
            _globalTumorCount.addAndGet(patient.getTumors().size());
        }

        private void handlePatient(Patient patient) {
            for (NaaccrValidationError error : patient.getAllValidationErrors()) {

                // file count
                AtomicInteger count = _warningCounts.get(error.getCode());
                if (count == null)
                    _warningCounts.put(error.getCode(), new AtomicInteger(1));
                else
                    count.incrementAndGet();

                // global count
                AtomicInteger globalCount = _globalCounts.get(error.getCode());
                if (globalCount == null)
                    _globalCounts.put(error.getCode(), new AtomicInteger(1));
                else
                    globalCount.incrementAndGet();

                if (error.getNaaccrId() != null) {

                    // file properties
                    _warningDetails.computeIfAbsent(error.getCode(), k -> new HashSet<>()).add(error.getNaaccrId());

                    // global properties
                    _globalDetails.computeIfAbsent(error.getCode(), k -> new HashSet<>()).add(error.getNaaccrId());
                }
            }
        }
    }

    public static String formatNumber(int num) {
        DecimalFormat format = new DecimalFormat();
        format.setDecimalSeparatorAlwaysShown(false);
        return format.format(num);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static String formatTime(long timeInMilli) {
        long hourBasis = 60;

        StringBuilder formattedTime = new StringBuilder();

        long secTmp = timeInMilli / 1000;
        long sec = secTmp % hourBasis;
        long minTmp = secTmp / hourBasis;
        long min = minTmp % hourBasis;
        long hour = minTmp / hourBasis;

        if (hour > 0) {
            formattedTime.append(hour).append(" hour");
            if (hour > 1)
                formattedTime.append("s");
        }

        if (min > 0) {
            if (formattedTime.length() > 0)
                formattedTime.append(", ");
            formattedTime.append(min).append(" minute");
            if (min > 1)
                formattedTime.append("s");
        }

        if (sec > 0) {
            if (formattedTime.length() > 0)
                formattedTime.append(", ");
            formattedTime.append(sec).append(" second");
            if (sec > 1)
                formattedTime.append("s");
        }

        if (formattedTime.length() > 0)
            return formattedTime.toString();

        return "< 1 second";
    }

    public static String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        else if (size < 1024 * 1024)
            return new DecimalFormat("#.# KB").format((double)size / 1024);
        else if (size < 1024 * 1024 * 1024)
            return new DecimalFormat("#.# MB").format((double)size / 1024 / 1024);

        return new DecimalFormat("#.# GB").format((double)size / 1024 / 1024 / 1024);
    }
}
