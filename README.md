Simple tool that tries to mitigate problems caused by [JT-80023](https://youtrack.jetbrains.com/issue/JT-80023/Blobs-are-not-cleaned-up-after-complete-removal-of-projects-or-issues).
It looks for blob files in youtrack DB, that are not linked from the database.
All such files can be optionally truncated to 0 bytes, to save disk space.

# How to build
```
./gradlew shadowJar
```

# Usage
```
java -classpath youtrack-db-cleaner-all.jar cz.seznam.youtrack.MainKt [<args>...] <path>
```

## Arguments
  `-h`, `--help`        Print this help.
  `-v`, `--verbose`     List all the analyzed files and some more information.
  `-d`, `--delete`      Truncate the blob files that should not be present.
                        Without this option, files are only listed and no destructive action is taken.
  `<path>`              Path to the database, e.g. '/opt/youtrack/data/youtrack'.

# IMPORTANT NOTES:
  1. This is not an official tool, you are using it at your own risk.
  2. Make a backup of your YouTrack before running with '--delete'.
  3. YouTrack instance must be stopped before running this.
