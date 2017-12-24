package droidefense.emulator.helper;


import droidefense.emulator.machine.base.DalvikVM;
import droidefense.emulator.machine.base.struct.generic.IDroidefenseClass;
import droidefense.log4j.Log;
import droidefense.log4j.LoggerType;
import droidefense.sdk.helpers.DroidDefenseEnvironment;
import droidefense.sdk.model.base.DroidefenseProject;
import droidefense.sdk.model.io.AbstractHashedFile;
import droidefense.sdk.model.io.DexHashedFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Created by sergio on 3/5/16.
 */
public class DexFileStatistics implements Serializable {

    private static final DroidDefenseEnvironment environment = DroidDefenseEnvironment.getInstance();

    private transient final DroidefenseProject currentProject;
    private transient final ArrayList<DexHashedFile> list;
    private transient final DalvikVM vm;

    //packages info
    private TreeSet<String> allPackages, realDeveloperPackages;
    //packages count
    private int totalPackageCount, realDeveloperPackagesCount;

    //class info
    private TreeSet<String> developerClassList, realDeveloperClassList, realDeveloperInnerClassList;

    //class count
    private int totalClassCount;
    private int developerClassCount, realDeveloperClassCount;
    private int realDeveloperInnerClassCount;

    public DexFileStatistics(DroidefenseProject currentProject) {
        //create information holders
        allPackages = new TreeSet<>();
        realDeveloperPackages = new TreeSet<>();
        developerClassList = new TreeSet<>();
        realDeveloperClassList = new TreeSet<>();
        realDeveloperInnerClassList = new TreeSet<>();
        //create VM
        vm = currentProject.getDalvikMachine();
        this.currentProject = currentProject;
        this.list = currentProject.getDexList();
        this.list.forEach(this::process);
    }

    private void process(AbstractHashedFile dexFile) {
        byte[] data;
        try {
            data = dexFile.getContent();
            ///then use default class loader
            vm.load(dexFile, data, DalvikVM.MULTIDEX);
            //once file is loaded, we can read the info

            //1 generate package names

            //2 get the classnames of developer handmade
            for (IDroidefenseClass cls : currentProject.getInternalInfo().getAllClasses()) {
                String full = cls.getAndroifiedClassName();
                allPackages.add(full);
                if (!cls.isAndroidv4v7Class()) {
                    realDeveloperPackages.add(full);
                }
                if (environment.isDeveloperClass(cls)) {
                    developerClassList.add(cls.getAndroifiedClassName());
                }
            }
            //1.1 count packages
            totalPackageCount = allPackages.size();
            realDeveloperPackagesCount = realDeveloperPackages.size();
            ;

            //2.1 count total class and dev class
            this.developerClassCount = developerClassList.size();
            this.totalClassCount = currentProject.getInternalInfo().getAllClasses().length;

            //2.2 remove autogeneratedClasses
            String[] junk = IDroidefenseClass.getAndroidRClasses();

            //2.1 count total class and dev class
            for (String clsName : developerClassList) {
                boolean notFound = false;
                for (String s : junk) {
                    notFound = clsName.endsWith(s);
                    if (notFound) {
                        break;
                    }
                }
                if (!notFound) {
                    if (clsName.contains("$")) {
                        //inner class
                        realDeveloperInnerClassList.add(clsName);
                    } else {
                        //normal class
                        realDeveloperClassList.add(clsName);
                    }
                }
            }

            //count classes
            this.realDeveloperClassCount = realDeveloperClassList.size();
            //count inner classes
            this.realDeveloperInnerClassCount = realDeveloperInnerClassList.size();
            Log.write(LoggerType.TRACE, ".dex statistics done");

        } catch (IOException e) {
            Log.write(LoggerType.ERROR, "Error while processing .dex file", e.getLocalizedMessage());
        }
    }
}