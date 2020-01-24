Prass
=======
Prass is an Android app designed to print via an SSH connection.

It was developed for using printers at the CIP pools at the math department of LMU Munich. However, it can easily be changed to work for any setup where you have SSH access to a server that can print via the Unix `lp` command.

<img src="Screenshot_statistics.png" width=300/> <img src="Screenshot_print.png" width=300/>

Some Details
------------
When a file is sent for printing, it's copied (via `scp`) to the server, where the `lp` command is executed via `ssh` to create a print job. The copy of the file is deleted afterwards.

The app supports password-protected PDFs and choosing some standard printer job options (two-sided printing, multiple pages per sheet, printing multiple copies, giving a list of pages to be printed, ...) in a neat GUI.

It should be easy to adapt the app to your needs by changing the hardcoded printer URLs (note that in the provided code, one of two available servers is selected at random for rudimentary load balancing) and printer names.

Download
--------
Once you've cloned this repository, the app can be compiled from Android Studio, or by running

    ./gradlew assemble

in the root of the repository from a command line, provided you have the necessary Android frameworks installed (this can fail if you're using JDK 13; try running it with JDK 8).
