package utils

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

/**
 * Class with various functions related to filesystem
 */
object FileUtils {

    /**
     * Function returns size of provided file
     * @param path - Path to file
     * @return Size of file in bytes
     */
    private val folderSizeFunction = { path: Path ->
        try {
            Files.size(path)
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Function which used to increment count of files by one
     */
    private val folderCountFunction = { path: Path -> 1L }

    /**
     * Function used as operator in stream to sum values
     */
    private val sumReducer = { sum:Long, value:Long -> sum + value }

    /**
     * Function used to remove folder and its content recursively
     * @param path: Path to folder
     * @param ifEmpty : If true, then removes subfolders only if they are empty, otherwise delete everything
     */
    fun removeFolder(path: Path, ifEmpty: Boolean) {
        if (Files.notExists(path)) return
        try {
            Files.walk(path).sorted(Comparator.comparingInt<Path>(Path::getNameCount).reversed()).forEach { file ->
                if (Files.isDirectory(file)) {
                    val count = getFolderFilesCount(file)
                    if (count == 0L) {
                        try {
                            Files.delete(file)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                } else if (!ifEmpty) {
                    try {
                        Files.delete(file)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * Function returns number of entries in specified folder (including files and subfolders)
     * @param path - Path to folder
     * @return number of records
     */
    fun getFolderFilesCount(path: Path): Long {
        return getFolderMetric(path, folderCountFunction, sumReducer)!! - 1
    }

    /**
     * Function returns summary size of all files inside folder
     * @param path - Path to folder
     * @return Size of files in folder in bytes
     */
    fun getFolderFilesSize(path: Path): Long {
        return getFolderMetric(path, folderSizeFunction, sumReducer)!!
    }

    /**
     * Base function used to calculate something related to content in folder
     * @param path Path to folder
     * @param func Function which gets value related to each file to calculate (map)
     * @param operator Function used to calculate value related to all files (reduce)
     * @return Calculated value
     */
    private fun getFolderMetric(path: Path, func: (Path)->Long, operator: (Long,Long)->Long): Long? {
        if (Files.notExists(path)) return 0L
        try {
            return Files.walk(path).map(func).reduce{s,s1-> operator(s,s1) }.orElse(0L)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return 0L
    }
}