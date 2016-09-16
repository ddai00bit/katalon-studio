package com.kms.katalon.composer.components.impl.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;

import com.kms.katalon.composer.components.impl.constants.ImageConstants;
import com.kms.katalon.composer.components.impl.constants.StringConstants;
import com.kms.katalon.composer.components.impl.transfer.TreeEntityTransfer;
import com.kms.katalon.composer.components.impl.util.TreeEntityUtil;
import com.kms.katalon.composer.components.tree.ITreeEntity;
import com.kms.katalon.composer.components.tree.TooltipPropertyDescription;
import com.kms.katalon.controller.FolderController;
import com.kms.katalon.entity.file.FileEntity;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.folder.FolderEntity.FolderType;
import com.kms.katalon.groovy.util.GroovyUtil;

public class FolderTreeEntity extends AbstractTreeEntity {

    private static final long serialVersionUID = 2019661366633516087L;

    private FolderEntity folder;

    public FolderTreeEntity(FolderEntity folder, ITreeEntity parentTreeEntity) {
        super(folder, parentTreeEntity);
        this.folder = folder;
    }

    @Override
    public Object[] getChildren() throws Exception {
        if (folder.getFolderType() == FolderType.KEYWORD) {
            List<Object> childrenEntities = new ArrayList<Object>();

            for (IPackageFragment packageFragment : GroovyUtil.getAllPackageInKeywordFolder(folder.getProject())) {
                if (packageFragment.exists()) {
                    childrenEntities.add(new PackageTreeEntity(packageFragment, this));
                }
            }
            return childrenEntities.toArray();
        }

        return TreeEntityUtil.getChildren(this);
    }

    @Override
    public boolean hasChildren() throws Exception {
        return true;
    }

    @Override
    public Image getImage() throws Exception {
        final String idForDisplay = folder.getIdForDisplay();
        if (StringConstants.ROOT_FOLDER_NAME_TEST_CASE.equals(idForDisplay)) {
            return ImageConstants.IMG_16_FOLDER_TEST_CASE;
        }
        if (StringConstants.ROOT_FOLDER_NAME_TEST_SUITE.equals(idForDisplay)) {
            return ImageConstants.IMG_16_FOLDER_TEST_SUITE;
        }
        if (StringConstants.ROOT_FOLDER_NAME_OBJECT_REPOSITORY.equals(idForDisplay)) {
            return ImageConstants.IMG_16_FOLDER_OBJECT;
        }
        if (StringConstants.ROOT_FOLDER_NAME_DATA_FILE.equals(idForDisplay)) {
            return ImageConstants.IMG_16_FOLDER_DATA;
        }
        if (StringConstants.ROOT_FOLDER_NAME_KEYWORD.equals(idForDisplay)) {
            return ImageConstants.IMG_16_FOLDER_KEYWORD;
        }
        if (StringConstants.ROOT_FOLDER_NAME_REPORT.equals(idForDisplay)) {
            return ImageConstants.IMG_16_FOLDER_REPORT;
        }
        if (StringConstants.ROOT_FOLDER_NAME_CHECKPOINT.equals(idForDisplay)) {
            return ImageConstants.IMG_16_FOLDER_CHECKPOINT;
        }
        return ImageConstants.IMG_16_FOLDER;
    }

    @Override
    public String getTypeName() throws Exception {
        return StringConstants.TREE_FOLDER_TYPE_NAME;
    }

    @Override
    public boolean isRemoveable() throws Exception {
        final String idForDisplay = folder.getIdForDisplay();
        return (idForDisplay.equals(StringConstants.ROOT_FOLDER_NAME_TEST_CASE)
                || idForDisplay.equals(StringConstants.ROOT_FOLDER_NAME_TEST_SUITE)
                || idForDisplay.equals(StringConstants.ROOT_FOLDER_NAME_OBJECT_REPOSITORY)
                || idForDisplay.equals(StringConstants.ROOT_FOLDER_NAME_DATA_FILE)
                || idForDisplay.equals(StringConstants.ROOT_FOLDER_NAME_KEYWORD)
                || idForDisplay.equals(StringConstants.ROOT_FOLDER_NAME_REPORT) || idForDisplay.equals(StringConstants.ROOT_FOLDER_NAME_CHECKPOINT));
    }

    @Override
    public Object getObject() throws Exception {
        return folder;
    }

    @Override
    public boolean isRenamable() throws Exception {
        if (folder.getFolderType() == FolderType.REPORT) {
            return false;
        }
        return isRemoveable();
    }

    @Override
    public Transfer getEntityTransfer() throws Exception {
        return TreeEntityTransfer.getInstance();
    }

    @Override
    public String getCopyTag() throws Exception {
        return folder.getFolderType().toString();
    }

    @Override
    public void setObject(Object object) throws Exception {
        if (object instanceof FolderEntity) {
            entity = (FileEntity) object;
            folder = (FolderEntity) object;
        }
    }

    @Override
    public String getKeyWord() throws Exception {
        switch (folder.getFolderType()) {
            case TESTCASE:
                return TestCaseTreeEntity.KEY_WORD;
            case TESTSUITE:
                return TestSuiteTreeEntity.KEY_WORD;
            case DATAFILE:
                return TestDataTreeEntity.KEY_WORD;
            case KEYWORD:
                return KeywordTreeEntity.KEY_WORD;
            case WEBELEMENT:
                return WebElementTreeEntity.KEY_WORD;
            case REPORT:
                return ReportTreeEntity.KEY_WORD;
            case CHECKPOINT:
                return CheckpointTreeEntity.KEY_WORD;
            default:
                return StringUtils.EMPTY;
        }
    }

    @Override
    public String[] getSearchTags() throws Exception {
        switch (folder.getFolderType()) {
            case TESTCASE:
                return TestCaseTreeEntity.SEARCH_TAGS;
            case TESTSUITE:
                return TestSuiteTreeEntity.SEARCH_TAGS;
            case DATAFILE:
                return TestDataTreeEntity.SEARCH_TAGS;
            case KEYWORD:
                return KeywordTreeEntity.SEARCH_TAGS;
            case WEBELEMENT:
                return WebElementTreeEntity.SEARCH_TAGS;
            case REPORT:
                return ReportTreeEntity.SEARCH_TAGS;
            case CHECKPOINT:
                return CheckpointTreeEntity.SEARCH_TAGS;
            default:
                return null;
        }
    }

    @Override
    public String getPropertyValue(String key) {
        if (key.equals("name")) {
            return folder.getName();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public Image getEntryImage() throws Exception {
        switch (folder.getFolderType()) {
            case TESTCASE:
                return ImageConstants.IMG_16_TEST_CASE;
            case TESTSUITE:
                return ImageConstants.IMG_16_TEST_SUITE;
            case DATAFILE:
                return ImageConstants.IMG_16_TEST_DATA;
            case KEYWORD:
                return ImageConstants.IMG_16_KEYWORD;
            case WEBELEMENT:
                return ImageConstants.IMG_16_TEST_OBJECT;
            case REPORT:
                return ImageConstants.IMG_16_REPORT;
            case CHECKPOINT:
                return ImageConstants.IMG_16_CHECKPOINT;
            default:
                return null;
        }
    }

    public void loadAllDescentdantEntities() throws Exception {
        FolderController.getInstance().loadAllDescentdantEntities(folder);
    }

    @Override
    public List<TooltipPropertyDescription> getTooltipDescriptions() {
        return Collections.emptyList();
    }
}
