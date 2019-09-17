package com.kms.katalon.composer.explorer.providers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.Viewer;

import com.kms.katalon.composer.components.impl.providers.AbstractEntityViewerFilter;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.tree.ITreeEntity;
import com.kms.katalon.composer.explorer.parts.ExplorerPart;
import com.kms.katalon.controller.FilterController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.file.FileEntity;
import com.kms.katalon.execution.setting.ExplorerSettingStore;

public class EntityViewerFilter extends AbstractEntityViewerFilter {

    private String searchString;

    private EntityProvider entityProvider;
    
    private ExplorerSettingStore store;

    public EntityViewerFilter(EntityProvider entityProvider) {
        this.entityProvider = entityProvider;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    @SuppressWarnings("restriction")
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (store == null) {
            try {
                store = new ExplorerSettingStore(ProjectController.getInstance().getCurrentProject());
            } catch(Exception e) {
                LoggerSingleton.getInstance().getLogger().error(e);
            }
        }

        if (element instanceof ITreeEntity && store != null) {
            try {
                String entityName = ((ITreeEntity) element).getText();
                boolean isShow = store.isItemShow(entityName);
                if (!isShow) {
                   return false;
                }
            } catch (Exception e) {
                LoggerSingleton.getInstance().getLogger().error(e);
            }		
        }

        if (searchString == null || searchString.equals(StringUtils.EMPTY)) {
            return true;
        }
        if (element instanceof ITreeEntity) {
            boolean returnValue = false;
            try {
                returnValue |= searchElement(element);
            } catch (Exception e) {
                LoggerSingleton.getInstance().getLogger().error(e);
            }

            if (returnValue) {
                return true;
            }
            ITreeEntity entity = ((ITreeEntity) element);
            try {
                if (searchString.startsWith(ExplorerPart.KEYWORD_SEARCH_ALL)
                        || searchString.startsWith(entity.getKeyWord())) {
                    if (entityProvider.getChildren(element) != null) {
                        for (Object child : entityProvider.getChildren(element)) {
                            if (child != null) {
                                returnValue |= select(viewer, element, child);
                            }
                        }
                    }
                    return returnValue;
                }
            } catch (Exception e) {
                return false;
            }

        }
        return false;
    }

    /**
     * filter all tree elements by searched string
     * 
     * @param element
     * is a instance of ITreeEntity
     * @return
     */
    private boolean searchElement(Object element) {
        try {
            ITreeEntity entity = ((ITreeEntity) element);
            // entity keyword
            String keyWord = entity.getKeyWord() + ":";
            String regex = "^" + keyWord + ".*$";

            // keyword all
            String keyWordAll = ExplorerPart.KEYWORD_SEARCH_ALL + ":";
            String regexKeyWordAll = "^" + keyWordAll + ".*$";

            String contentString = searchString.trim();
            if (searchString.matches(regex) || searchString.matches(regexKeyWordAll)) {
                // cut keyword
                if (searchString.matches(regex)) {
                    contentString = contentString.substring(keyWord.length()).trim();
                } else {
                    contentString = contentString.substring(keyWordAll.length()).trim();
                }

                if (contentString.isEmpty()) {
                    return true;
                }

                if (entity.getText().toLowerCase().contains(contentString.toLowerCase())) {
                    return true;
                }

                FilterController folderController = FilterController.getInstance();
                List<String> keywordList = folderController.getDefaultKeywords();
                Map<String, String> tagMap = parseSearchedString(keywordList.toArray(new String[0]), contentString);

                if (tagMap != null && !tagMap.isEmpty() && entity.getObject() instanceof FileEntity) {
                    FileEntity fileEntity = (FileEntity) entity.getObject();
                    for (Entry<String, String> entry : tagMap.entrySet()) {
                        String keyword = entry.getKey();
                        if (folderController.getDefaultKeywords().contains(keyword) 
                                && !folderController.compare(fileEntity, keyword, entry.getValue())) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            LoggerSingleton.logError(e);
        }
        return false;
    }

    /**
     * parse searched string into a map of search tags of an entity element
     * 
     * @param element
     * is ITreeEntity
     * @return
     */
    @SuppressWarnings("restriction")
    public static Map<String, String> parseSearchedString(String[] searchTags, String contentString) {
        try {
            if (searchTags != null) {
                Map<String, String> tagMap = new HashMap<String, String>();
                for (int i = 0; i < searchTags.length; i++) {
                    String tagRegex = searchTags[i] + "=\\([^\\)]+\\)";
                    Matcher m = Pattern.compile(tagRegex).matcher(contentString);
                    while (m.find()) {
                        String tagContent = contentString.substring(m.start() + searchTags[i].length() + 2,
                                m.end() - 1);
                        tagMap.put(searchTags[i], tagContent);
                    }
                }
                return tagMap;
            } else {
                return null;
            }

        } catch (Exception e) {
            LoggerSingleton.getInstance().getLogger().error(e);
            return null;
        }
    }
}
