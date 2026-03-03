package unicredit.logger;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentException;
import org.apache.ofbiz.base.util.Debug;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class UnicreditLogger {

    public static final String MODULE = UnicreditLogger.class.getName();

    private final static String UNICREDIT_COOMPONENT = "unicredit";
    private final static String LOGDIR = "LOG";
    private final static String LOG_FILE_PREFIX = "UNICREDIT_";
    private final static String LOG_FILE_EXT = ".log";
    private final static String INFO_MSG_PREFIX = "[ I ] ";
    private final static String WARN_MSG_PREFIX = "[ W ] ";
    private final static String ERR_MSG_PREFIX = "[ E ] ";
    private static Calendar calendar = Calendar.getInstance();
    private static Path logFile = null;

    // constructor
    public UnicreditLogger(String tenantId) {

        String componentPath = null;
        String tenantLogDirStr = null;
        boolean tenantLogDirExists = true;
        boolean mainLogDirExists = true;

        try {

            componentPath = ComponentConfig.getRootLocation(UNICREDIT_COOMPONENT);

        } catch (ComponentException ce) {
            Debug.logError(ce, MODULE);
        }

        String componentLogDirPathStr = null;
        Path componentLogDirPath = null;

        if (componentPath.endsWith("/")) {
            componentLogDirPathStr = componentPath + LOGDIR;
            componentLogDirPath = Paths.get(componentLogDirPathStr);
        } else {
            componentLogDirPathStr = componentPath + "/" + LOGDIR;
            componentLogDirPath = Paths.get(componentLogDirPathStr);
        }

        tenantLogDirStr = componentLogDirPathStr + "/" + tenantId;
        Path tenantLogDirPath = Paths.get(tenantLogDirStr);

        if (checkFileDirNotExists(tenantLogDirPath)) {
            tenantLogDirExists = createNewDir(tenantLogDirPath);
        }

        /*
         * if the tenant log directory exists, then create a log file for now date if
         * does not exists yet; if the file for actual date already exists, then we will
         * use that for logging. The file name is MSP_YYYYMMDD.log
         */
        if (tenantLogDirExists) {

            String logFilePathStr = tenantLogDirStr + "/" + getFileName();
            Debug.logWarning("log file path: " + logFilePathStr, MODULE);
            Path logFilePath = Paths.get(logFilePathStr);

            // check if the file does not exists
            if (checkFileDirNotExists(logFilePath)) {
                this.logFile = createNewFile(logFilePath);

                try {
                    Files.write(this.logFile, getFileHeaderMessage().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException ioe) {
                    Debug.logError(ioe, MODULE);
                }

            } else {
                /*
                 * if the file already exists, set the reference to it or
                 * NullPointerExceptionError will be thrown
                 */
                if (this.logFile == null) {
                    this.logFile = logFilePath;
                }

            }

        }

    } // end class constructor

    /**
     * Method that write a log message for a "INFO" log level.
     *
     * @param msg the message to write into the log file
     */
    public void logInfo(String msg) {

        msg = INFO_MSG_PREFIX + msg + "\n";

        try {
            Files.write(this.logFile, msg.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            Debug.logError(ioe, MODULE);
        }

        return;

    }

    /**
     * Method that write a log message for a "WARNING" log level.
     *
     * @param msg the message to write into the log file
     */
    public void logWarning(String msg) {

        msg = WARN_MSG_PREFIX + msg + "\n";

        try {
            Files.write(this.logFile, msg.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            Debug.logError(ioe, MODULE);
        }

        return;

    }

    /**
     * Method that write a log message for a "ERROR" log level.
     *
     * @param msg the message to write into the log file
     */
    public void logError(String msg) {

        msg = ERR_MSG_PREFIX + msg + "\n";

        try {
            Files.write(this.logFile, msg.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            Debug.logError(ioe, MODULE);
        }

        return;

    }

    /* ############################# PRIVATE METHODS ############################ */

    /**
     * Method that checks if a file or directory exists.
     *
     * @param path the abstract pathname of the directory to checks
     * @return true if directory exists; false otherwise.
     */
    private boolean checkFileDirExists(Path path) {
        return Files.exists(path, new LinkOption[] { LinkOption.NOFOLLOW_LINKS });
    }

    /**
     * Method that checks if a file or directory does not exists.
     *
     * @param path the abstract pathname of the directory to checks
     * @return true if directory does not exists; false otherwise.
     */
    private boolean checkFileDirNotExists(Path path) {
        return Files.notExists(path, new LinkOption[] { LinkOption.NOFOLLOW_LINKS });
    }

    /**
     * Method that creates a new directory with 666 Posix file permissions.
     *
     * @param path of the directory to create
     * @return true if the directory has been created; false otherwise;
     */
    private boolean createNewDir(Path dirpath) {

        Path newDir = null;

        Set<PosixFilePermission> permSet = new HashSet<>();
        // add rw-rw-rw- (666) for the dir
        permSet.add(PosixFilePermission.OWNER_READ);
        permSet.add(PosixFilePermission.OWNER_WRITE);
        permSet.add(PosixFilePermission.OWNER_EXECUTE);
        permSet.add(PosixFilePermission.GROUP_READ);
        permSet.add(PosixFilePermission.GROUP_WRITE);
        permSet.add(PosixFilePermission.GROUP_EXECUTE);
        permSet.add(PosixFilePermission.OTHERS_READ);
        permSet.add(PosixFilePermission.OTHERS_WRITE);
        permSet.add(PosixFilePermission.OTHERS_EXECUTE);

        FileAttribute<Set<PosixFilePermission>> filePermission = PosixFilePermissions.asFileAttribute(permSet);

        try {
            newDir = Files.createDirectories(dirpath, filePermission);
        } catch (FileAlreadyExistsException faee) {
            Debug.logWarning("Directory " + dirpath + " already exists", MODULE);
            return true;
        } catch (UnsupportedOperationException uoe) {
            Debug.logError(uoe, MODULE);
            return false;
        } catch (SecurityException se) {
            Debug.logError(se, MODULE);
            return false;
        } catch (IOException ioe) {
            Debug.logError("Error in creating directory: " + dirpath + "." + ioe, MODULE);
            return false;
        }

        Debug.logWarning("create directory:" + newDir, MODULE);

        return Files.exists(newDir, new LinkOption[] { LinkOption.NOFOLLOW_LINKS });

    }

    /**
     * Method that creates a new file with 666 Posix file permissions.
     *
     * @param filepath of the file to create
     * @return the Path to the created file; null if errors occours;
     */
    private Path createNewFile(Path filepath) {

        Path newFile = null;

        Set<PosixFilePermission> permSet = new HashSet<>();
        // add rw-rw-rw- (666) for the dir
        permSet.add(PosixFilePermission.OWNER_READ);
        permSet.add(PosixFilePermission.OWNER_WRITE);
        permSet.add(PosixFilePermission.OWNER_EXECUTE);
        permSet.add(PosixFilePermission.GROUP_READ);
        permSet.add(PosixFilePermission.GROUP_WRITE);
        permSet.add(PosixFilePermission.GROUP_EXECUTE);
        permSet.add(PosixFilePermission.OTHERS_READ);
        permSet.add(PosixFilePermission.OTHERS_WRITE);
        permSet.add(PosixFilePermission.OTHERS_EXECUTE);
        FileAttribute<Set<PosixFilePermission>> filePermission = PosixFilePermissions.asFileAttribute(permSet);

        try {
            newFile = Files.createFile(filepath, filePermission);
        } catch (FileAlreadyExistsException faee) {
            Debug.logWarning("File " + filepath + " already exists", MODULE);
            return null;
        } catch (UnsupportedOperationException uoe) {
            Debug.logError(uoe, MODULE);
            return null;
        } catch (SecurityException se) {
            Debug.logError(se, MODULE);
            return null;
        } catch (IOException ioe) {
            Debug.logError("Error in creating file: " + filepath + "." + ioe, MODULE);
            return null;
        }

        return newFile;

    }

    /**
     * Method that build a file name for the log file. The file name has this form:
     * MSP_YYYYMMDD.log
     *
     * @return the log file name string
     */
    private String getFileName() {

        // set the current time for Calendar
        calendar.setTimeInMillis(System.currentTimeMillis());

        String year = Integer.toString(calendar.get(Calendar.YEAR));
        // Calendar.MONTH returns a number between 0 (Jan) to 11 (Dec)
        String month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));

        String filename = null;

        filename = LOG_FILE_PREFIX + year + month + day + LOG_FILE_EXT;
        Debug.logWarning("########## filename: " + filename, MODULE);

        return filename;

    }

    /**
     * Return a string composed this way: YYYY/MM/DD_h:m:s.
     *
     * @return a composed date time string for now
     */
    private String getNowDateTimeString() {

        // set the current time for Calendar
        calendar.setTimeInMillis(System.currentTimeMillis());

        String year = Integer.toString(calendar.get(Calendar.YEAR));
        // Calendar.MONTH returns a number between 0 (Jan) to 11 (Dec)
        String month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));

        String hour = Integer.toString(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = Integer.toString(calendar.get(Calendar.MINUTE));
        String sec = Integer.toString(calendar.get(Calendar.SECOND));

        String date_string = day + "/" + month + "/" + year + "_" + hour + ":" + minute + ":" + sec;

        return date_string;

    }


    /**
     * Return a header string to write into the log file, the first time is created.
     *
     * @return a header string composed by now date and time
     */
    private String getFileHeaderMessage() {
        String header_msg = "########## " + getNowDateTimeString() + " ##########\n";
        return header_msg;
    }

} //end class
