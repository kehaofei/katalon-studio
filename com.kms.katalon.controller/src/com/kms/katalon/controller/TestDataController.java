package com.kms.katalon.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.kms.katalon.dal.state.DataProviderState;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.testdata.DataFileEntity;
import com.kms.katalon.entity.testdata.DataFilePropertyInputEntity;
import com.kms.katalon.entity.testdata.InternalDataFilePropertyEntity;

@Creatable
public class TestDataController extends EntityController {

	private static EntityController _instance;

	private TestDataController() {
		super();
	}

	public static TestDataController getInstance() {
		if (_instance == null) {
			_instance = new TestDataController();
		}
		return (TestDataController) _instance;
	}

	public DataFileEntity addDataFile(Object parentEntity) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().addNewDataFile(getFolder(parentEntity));
	}

	public DataFileEntity saveDataFile(DataFileEntity newDataFile, FolderEntity parentFolder) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().saveDataFile(newDataFile);
	}

	public List<DataFileEntity> getDataFileFromParentFolder(FolderEntity parentFolder) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().getDataFileByFolder(parentFolder);
	}

	private FolderEntity getFolder(Object parentEntity) throws Exception {
		if (parentEntity instanceof FolderEntity) {
			return (FolderEntity) parentEntity;
		} else if (parentEntity instanceof DataFileEntity) {
			return ((DataFileEntity) parentEntity).getParentFolder();
		} else {
			return FolderController.getInstance().getTestDataRoot(DataProviderState.getInstance().getCurrentProject());
		}
	}

	public DataFileEntity copyDataFile(DataFileEntity dataFile, FolderEntity targetFolder) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().copyDataFile(dataFile, targetFolder);
	}

	public DataFileEntity moveDataFile(DataFileEntity dataFile, FolderEntity targetFolder) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().moveDataFile(dataFile, targetFolder);
	}

	public void deleteDataFile(DataFileEntity dataFile) throws Exception {
		dataProviderSetting.getDataFileDataProvider().deleteDataFile(dataFile);
	}

	public DataFileEntity updateDataFile(DataFilePropertyInputEntity dataFileInputProperties) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().updateDataFileProperty(dataFileInputProperties);
	}

	public DataFileEntity updateDataFile(InternalDataFilePropertyEntity internalDataFileInputProperties)
			throws Exception {
		return dataProviderSetting.getDataFileDataProvider().updateInternalDataFileProperty(
				internalDataFileInputProperties);

	}

	public DataFileEntity renameDataFile(DataFileEntity dataFile, String newName) throws Exception {
		DataFilePropertyInputEntity dataFileInputProperties = new DataFilePropertyInputEntity(dataFile);
		dataFileInputProperties.setName(newName);
		return dataProviderSetting.getDataFileDataProvider().updateDataFileProperty(dataFileInputProperties);
	}

	public String getIdForDisplay(DataFileEntity entity) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().getIdForDisplay(entity).replace("\\", "/");
	}

	public List<String> getSibblingDataFileNames(DataFileEntity dataFile) throws Exception {
		List<DataFileEntity> sibblingDataFiles = getDataFileFromParentFolder(dataFile.getParentFolder());
		List<String> sibblingName = new ArrayList<String>();
		for (DataFileEntity sibblingDataFile : sibblingDataFiles) {
			if (!dataProviderSetting.getEntityPk(sibblingDataFile).equals(dataProviderSetting.getEntityPk(dataFile))) {
				sibblingName.add(sibblingDataFile.getName());
			}
		}
		return sibblingName;
	}

	public String getAvailableTestDataName(FolderEntity parentFolder, String name) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().getAvailableDataFileName(parentFolder, name);
	}

	/**
	 * validate name of new test date before saving to file
	 * 
	 * @param parentFolder
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public boolean validateTestDataName(FolderEntity parentFolder, String name) throws Exception {
		return dataProviderSetting.getDataFileDataProvider().validateDataFileName(parentFolder, name);
	}
	
	public DataFileEntity getTestDataByDisplayId(String displayId) throws Exception {
	    String relativePathWithoutExtensions = displayId.replace("/", "\\");
	    return dataProviderSetting.getDataFileDataProvider().getDataFileByDisplayId(relativePathWithoutExtensions);
	}
	
	public DataFileEntity getTestData(String pk) throws Exception {
	    return dataProviderSetting.getDataFileDataProvider().getDataFile(pk);
	}
	
	public String getTestDataDisplayIdByPk(String pk, String projectLocation) {
	    return FilenameUtils.removeExtension(pk).replace(projectLocation + "\\", "").replace("\\", "/");
	}
}