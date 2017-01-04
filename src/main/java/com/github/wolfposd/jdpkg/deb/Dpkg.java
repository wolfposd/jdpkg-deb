/*
 * Copyright (C) 2017 wolfposd
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.wolfposd.jdpkg.deb;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

public class Dpkg {

    public static String BuildFile;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("jdpkg-deb: Missing parameters");
            System.out.println("jdpkg-deb: Usage: java -jar jdpkg.jar -Zgzip -b SomeFileOrFolder");
            System.exit(0);
        }

        BuildFile = getBuildfile(args);
        String compressType = getCompressionType(args, "gzip");

        if (!new File(BuildFile).exists()) {
            System.out.println("jdpkg-deb: error: failed to open package info file '" + BuildFile
                    + "/DEBIAN/control' for reading: No such file or directory");
            System.exit(0);
        }

        switch (compressType) {
            case "gzip":
                dpgk_deb(new File(BuildFile), ArchiveStreamFactory.TAR, CompressorStreamFactory.GZIP);
                break;
            default:
                System.out.println("Currently only supports gzip");
                break;

        }

    }

    public static boolean dpgk_deb(File folder, String archivetype, String compressionType) {

        String packageName = getPackageName();

        System.out.println("jdpkg-deb: building package '" + packageName + "' in '" + folder + ".deb'.");

        File dataTar = new File("data." + archivetype);
        File dataTarGz = new File(dataTar.getName() + "." + compressionType);
        File controlTar = new File("control." + archivetype);
        File controlTarGz = new File(controlTar.getName() + "." + compressionType);
        File debian_binary = new File("debian-binary");

        try {
            addFilesFromFolderToTar(folder, dataTar, ArchiveStreamFactory.TAR);

            if (dataTar.exists()) {
                compressArchive(dataTar, dataTarGz, compressionType);
                if (dataTarGz.exists()) {
                    dataTar.delete();
                }
            }

        } catch (IOException | ArchiveException | CompressorException e) {
            e.printStackTrace();
        }

        try {
            addFilesFromFolderToTar(new File(folder, "DEBIAN/control"), controlTar, ArchiveStreamFactory.TAR);

            if (controlTar.exists()) {
                compressArchive(controlTar, controlTarGz, compressionType);
                if (controlTarGz.exists()) {
                    controlTar.delete();
                }
            }

        } catch (IOException | ArchiveException | CompressorException e) {
            e.printStackTrace();
        }

        writeDebianBuildFile(debian_binary);

        if (dataTarGz.exists() && controlTarGz.exists()) {
            try {
                File debFile = new File(BuildFile + ".deb");
                writeDebFile(debFile, new File[] { controlTarGz, dataTarGz, debian_binary });

                if (debFile.exists()) {
                    controlTarGz.delete();
                    dataTarGz.delete();
                    debian_binary.delete();
                }

            } catch (ArchiveException | IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private static void compressArchive(File source, File destination, String compression) throws CompressorException, IOException {
        OutputStream archiveStream = new FileOutputStream(destination);
        CompressorOutputStream comp = new CompressorStreamFactory().createCompressorOutputStream(compression, archiveStream);

        BufferedInputStream input = new BufferedInputStream(new FileInputStream(source));

        IOUtils.copy(input, comp);
        input.close();
        comp.close();

    }

    private static void addFilesFromFolderToTar(final File source, final File destination, final String archiverName)
            throws IOException, ArchiveException {
        OutputStream archiveStream = new FileOutputStream(destination);
        ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(archiverName, archiveStream);

        if (source.isDirectory()) {
            for (File file : source.listFiles()) {

                if (file.getName().equals("DEBIAN")) {
                    continue;
                } else if (file.isDirectory()) {
                    addFilesFromFolderToTar(file, destination, archiverName);
                } else {
                    fileEntryToDestination(file, archive, false);
                }

            }
        } else {
            fileEntryToDestination(source, archive, true);
        }

        archive.finish();
        archiveStream.close();
    }

    public static void fileEntryToDestination(File source, ArchiveOutputStream archive, boolean atRoot) throws IOException {

        TarArchiveEntry entry;
        if (atRoot) {
            entry = new TarArchiveEntry(source.getName());
        } else {
            entry = new TarArchiveEntry(source.getPath().replace(BuildFile, ""));
        }
        entry.setSize(source.length());
        entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);

        archive.putArchiveEntry(entry);

        BufferedInputStream input = new BufferedInputStream(new FileInputStream(source));
        IOUtils.copy(input, archive);

        input.close();
        archive.closeArchiveEntry();
    }

    public static void writeDebianBuildFile(File f) {
        try (FileWriter writer = new FileWriter(f)) {
            writer.write("2.0\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeDebFile(File destination, File[] inputsfiles) throws ArchiveException, IOException {
        ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.AR,
                new FileOutputStream(destination));

        for (File file : inputsfiles) {
            ArArchiveEntry entry = new ArArchiveEntry(file, file.getName());
            archive.putArchiveEntry(entry);

            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(input, archive);
            input.close();

            archive.closeArchiveEntry();
        }
        archive.finish();
        archive.close();

    }

    public static String getCompressionType(String[] args, String defaultResult) {
        for (String s : args) {
            if (s.startsWith("-Z")) {
                return s.substring(s.indexOf("-Z") + 2);
            }
        }
        return defaultResult;
    }

    public static String getBuildfile(String[] args) {
        String buildf = null;
        for (int i = 0; i < args.length; i++) {
            String cur = args[i];
            if (cur.equals("-b") || cur.equals("--build")) {
                if (i + 1 < args.length) {
                    buildf = args[i + 1];
                    break;
                } else {
                    throw new IllegalArgumentException("jdpkg-deb: error: --build needs a <directory> argument");
                }
            }
        }
        if (buildf == null)
            throw new IllegalArgumentException("jdpkg-deb: error: need an action option");
        else
            return buildf;
    }

    public static String getPackageName() {
        File controlFile = new File(BuildFile + "/DEBIAN/control");

        String packageName = BuildFile;

        try (Scanner scan = new Scanner(controlFile)) {
            String line = "";
            while (scan.hasNextLine()) {
                line = scan.nextLine();
                if (line.toLowerCase().startsWith("package:")) {
                    packageName = line.replace("Package:", "").trim();
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return packageName;
    }

}
