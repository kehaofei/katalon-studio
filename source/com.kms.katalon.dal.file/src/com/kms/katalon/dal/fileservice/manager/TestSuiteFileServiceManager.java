package com.kms.katalon.dal.fileservice.manager;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.kms.katalon.dal.TestSuiteCollectionDataProvider;
import com.kms.katalon.dal.exception.DALException;
import com.kms.katalon.dal.fileservice.EntityService;
import com.kms.katalon.dal.fileservice.FileServiceConstant;
import com.kms.katalon.dal.fileservice.constants.StringConstants;
import com.kms.katalon.dal.fileservice.dataprovider.setting.FileServiceDataProviderSetting;
import com.kms.katalon.dal.state.DataProviderState;
import com.kms.katalon.entity.dal.exception.DuplicatedFileNameException;
import com.kms.katalon.entity.file.FileEntity;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.entity.testsuite.TestSuiteEntity;

public class TestSuiteFileServiceManager {
    public static TestSuiteEntity getTestSuite(String testSuitePk) throws Exception {
        FileEntity entity = EntityFileServiceManager.get(new File(testSuitePk));
        if (entity instanceof TestSuiteEntity) {
            return (TestSuiteEntity) entity;
        }
        return null;
    }

    public static TestSuiteEntity initTestSuite(TestSuiteEntity testSuiteEntity) throws Exception {
        return testSuiteEntity;
    }

    public static void deleteTestSuite(TestSuiteEntity testSuite) throws Exception {
        EntityFileServiceManager.delete(testSuite);
        File script = getTestSuiteScriptFile(testSuite);
        if (script.exists()) {
            script.delete();
        }
        FolderFileServiceManager.refreshFolder(testSuite.getParentFolder());
    }

    public static TestSuiteEntity updateTestSuite(TestSuiteEntity testSuite) throws Exception {
        EntityService.getInstance().saveEntity(testSuite);
        return testSuite;
    }

    public static TestSuiteEntity renameTestSuite(String newName, TestSuiteEntity testSuite) throws DALException {
        try {
            validateData(testSuite);
            testSuite = resetParentForChildElement(testSuite);
            // Remove old name in cache, it will be added again when saving
            EntityService.getInstance().getEntityCache().remove(testSuite, true);
            File script = getTestSuiteScriptFile(testSuite);
            if (script.exists()) {
                script.renameTo(new File(testSuite.getParentFolder().getLocation(), newName + ".groovy"));
            }
            testSuite.setName(newName);
            EntityService.getInstance().saveEntity(testSuite);
            updateTestSuiteReferences(testSuite);
            FolderFileServiceManager.refreshFolder(testSuite.getParentFolder());
            return testSuite;
        } catch (Exception e) {
            throw new DALException(e);
        }
    }

    private static void updateTestSuiteReferences(TestSuiteEntity testSuite) throws DALException {
        TestSuiteCollectionDataProvider tsCollectionDataProvider = new FileServiceDataProviderSetting()
                .getTestSuiteCollectionDataProvider();
        tsCollectionDataProvider.updateTestSuiteCollectionReferences(testSuite, testSuite.getProject());
    }

    /**
     ** Check duplication of name (if name changed)
     **/
    public static void validateData(TestSuiteEntity testSuiteEntity) throws Exception {
        if (nameChanged(testSuiteEntity)) {
            // validate name
            EntityService.getInstance().validateName(testSuiteEntity.getName());
            // check duplicate name
            File file = new File(testSuiteEntity.getLocation());
            if (file.exists()) {
                throw new DuplicatedFileNameException(MessageFormat
                        .format(StringConstants.MNG_EXC_EXISTED_TEST_SUITE_NAME, testSuiteEntity.getName()));
            }
        }
    }

    public static TestSuiteEntity resetParentForChildElement(TestSuiteEntity testSuite) throws Exception {

        return testSuite;
    }

    private static boolean nameChanged(TestSuiteEntity testSuiteEntity) throws Exception {
        String pk = EntityService.getInstance().getEntityCache().getKey(testSuiteEntity);
        if (pk == null) {
            pk = getTestSuite(testSuiteEntity.getId()).getId();
        }
        String oldName = pk.substring(pk.lastIndexOf(File.separator) + 1,
                pk.indexOf(TestSuiteEntity.getTestSuiteFileExtension()));
        return !oldName.equalsIgnoreCase(testSuiteEntity.getName());
    }

    /**
     * Save a NEW Test Suite.<br>
     * Please user {@link #updateTestSuite(TestSuiteEntity)} if you want to save an existing Test Suite.
     * 
     * @param newTestSuite a new Test Suite which is created by {@link #newTestSuiteWithoutSave(FolderEntity, String)}
     * @return {@link TestSuiteEntity} the saved Test Suite
     * @throws Exception
     */
    public static TestSuiteEntity saveNewTestSuite(TestSuiteEntity newTestSuite) throws Exception {
        if (newTestSuite == null || newTestSuite.getProject() == null || newTestSuite.getParentFolder() == null) {
            return null;
        }

        EntityService.getInstance().saveEntity(newTestSuite);
        FolderFileServiceManager.refreshFolder(newTestSuite.getParentFolder());

        return newTestSuite;
    }

    public static String getAvailableTestSuiteName(FolderEntity parentFolder, String name) throws Exception {
        return EntityService.getInstance().getAvailableName(parentFolder.getLocation(), name, true);
    }

    public static TestSuiteEntity copyTestSuite(TestSuiteEntity testSuite, FolderEntity destinationFolder)
            throws Exception {
        TestSuiteEntity coppiedTestSuite = EntityFileServiceManager.copy(testSuite, destinationFolder);
        File script = getTestSuiteScriptFile(testSuite);
        if (script.exists()) {
            FileUtils.copyFile(script, getTestSuiteScriptFile(coppiedTestSuite));
        }
        return coppiedTestSuite;
    }

    public static TestSuiteEntity moveTestSuite(TestSuiteEntity testSuite, FolderEntity destinationFolder)
            throws Exception {
        // Maybe the testSuite parameter is cloned from the real, so we need to get the real one from system.
        TestSuiteEntity cachedTestSuite = (TestSuiteEntity) EntityService.getInstance()
                .getEntityByPath(testSuite.getId());
        File script = getTestSuiteScriptFile(testSuite);
        cachedTestSuite = EntityFileServiceManager.move(cachedTestSuite, destinationFolder);
        if (script.exists()) {
            FileUtils.moveFile(script, getTestSuiteScriptFile(cachedTestSuite));
        }
        updateTestSuiteReferences(cachedTestSuite);
        return cachedTestSuite;
    }

    public static TestSuiteEntity getTestSuiteByName(FolderEntity parentFolder, String testSuiteName) throws Exception {
        List<TestSuiteEntity> testSuites = FolderFileServiceManager.getChildTestSuitesOfFolder(parentFolder);
        for (TestSuiteEntity testSuite : testSuites) {
            if (testSuite.getName().equals(testSuiteName)) {
                return testSuite;
            }
        }
        return null;
    }

    public static TestSuiteEntity getTestSuiteByDisplayId(String testSuiteId) throws Exception {
        if (StringUtils.isBlank(testSuiteId)) {
            return null;
        }

        ProjectEntity projectEntity = DataProviderState.getInstance().getCurrentProject();
        String testSuitePk = projectEntity.getFolderLocation() + File.separator
                + testSuiteId.replace(StringConstants.ENTITY_ID_SEPARATOR, File.separator)
                + TestSuiteEntity.getTestSuiteFileExtension();
        return getTestSuite(testSuitePk);
    }

    public static TestSuiteEntity getByGUID(String guid, ProjectEntity project) throws Exception {
        File projectFolder = new File(project.getFolderLocation());
        if (projectFolder.exists() && projectFolder.isDirectory()) {
            File testSuiteFolder = new File(
                    FileServiceConstant.getTestSuiteRootFolderLocation(projectFolder.getAbsolutePath()));
            if (testSuiteFolder.exists() && testSuiteFolder.isDirectory()) {
                return getByGUID(testSuiteFolder.getAbsolutePath(), guid, project);
            }
        }
        return null;
    }

    private static TestSuiteEntity getByGUID(String testSuiteFolder, String guid, ProjectEntity project)
            throws Exception {
        File folder = new File(testSuiteFolder);
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles(EntityFileServiceManager.fileFilter)) {
                if (file.isFile() && file.getName()
                        .toLowerCase()
                        .endsWith(TestSuiteEntity.getTestSuiteFileExtension().toLowerCase())) {
                    FileEntity entity = EntityFileServiceManager.get(file);
                    if (entity instanceof TestSuiteEntity
                            && ((TestSuiteEntity) entity).getTestSuiteGuid().equals(guid)) {
                        return (TestSuiteEntity) entity;
                    }
                } else if (file.isDirectory()) {
                    TestSuiteEntity result = getByGUID(file.getAbsolutePath(), guid, project);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    public static File getTestSuiteScriptFile(TestSuiteEntity testSuite) throws DALException {
        return new File(testSuite.getProject().getFolderLocation(), testSuite.getIdForDisplay() + ".groovy");
    }

    public static File newTestSuiteScriptFile(TestSuiteEntity testSuite) throws DALException {
        File script = getTestSuiteScriptFile(testSuite);
        if (script.exists()) {
            return script;
        }
        try {
            script.createNewFile();
        } catch (IOException e) {
            throw new DALException(e);
        }
        return script;
    }
}