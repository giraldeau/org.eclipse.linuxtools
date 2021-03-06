/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Guzman - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.rpm.createrepo;

import org.eclipse.osgi.util.NLS;

/**
 * Messages displayed across the plugin.
 */
public final class Messages {

    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.rpm.createrepo.messages"; //$NON-NLS-1$

    // CreaterepoWizard
    /****/
    public static String CreaterepoWizard_errorCreatingProject;
    /****/
    public static String CreaterepoWizard_openFileOnCreation;
    /****/
    public static String CreaterepoWizard_errorOpeningNewlyCreatedFile;

    // CreaterepoNewWizardPageOne
    /****/
    public static String CreaterepoNewWizardPageOne_wizardPageName;
    /****/
    public static String CreaterepoNewWizardPageOne_wizardPageTitle;
    /****/
    public static String CreaterepoNewWizardPageOne_wizardPageDescription;

    // CreaterepoNewWizardPageTwo
    /****/
    public static String CreaterepoNewWizardPageTwo_wizardPageName;
    /****/
    public static String CreaterepoNewWizardPageTwo_wizardPageTitle;
    /****/
    public static String CreaterepoNewWizardPageTwo_wizardPageDescription;
    /****/
    public static String CreaterepoNewWizardPageTwo_labelID;
    /****/
    public static String CreaterepoNewWizardPageTwo_labelName;
    /****/
    public static String CreaterepoNewWizardPageTwo_labelURL;
    /****/
    public static String CreaterepoNewWizardPageTwo_errorID;
    /****/
    public static String CreaterepoNewWizardPageTwo_errorName;
    /****/
    public static String CreaterepoNewWizardPageTwo_errorURL;
    /****/
    public static String CreaterepoNewWizardPageTwo_tooltipID;
    /****/
    public static String CreaterepoNewWizardPageTwo_tooltipName;
    /****/
    public static String CreaterepoNewWizardPageTwo_tooltipURL;

    // CreaterepoProject
    /****/
    public static String CreaterepoProject_executeCreaterepo;
    /****/
    public static String CreaterepoProject_errorGettingFile;
    /****/
    public static String CreaterepoProject_consoleName;

    // Createrepo
    /****/
    public static String Createrepo_jobName;
    /****/
    public static String Createrepo_jobCancelled;
    /****/
    public static String Createrepo_errorExecuting;
    /****/
    public static String Createrepo_errorTryingToFindCommand;
    /****/
    public static String Createrepo_errorCommandNotFound;
    /****/
    public static String Createrepo_errorWrongVersionCreaterepo;
    /****/
    public static String Createrepo_errorWrongVersionYum;
    /****/
    public static String Createrepo_errorCancelled;
    /****/
    public static String Createrepo_errorPasingVersion;

    // RepoFormEditor
    /****/
    public static String RepoFormEditor_errorInitializingForm;
    /****/
    public static String RepoFormEditor_errorInitializingProject;

    // ImportRPMsPage
    /****/
    public static String ImportRPMsPage_title;
    /****/
    public static String ImportRPMsPage_formHeaderText;
    /****/
    public static String ImportRPMsPage_sectionTitle;
    /****/
    public static String ImportRPMsPage_sectionInstruction;
    /****/
    public static String ImportRPMsPage_buttonImportRPMs;
    /****/
    public static String ImportRPMsPage_buttonRemoveRPMs;
    /****/
    public static String ImportRPMsPage_buttonCreateRepo;
    /****/
    public static String ImportRPMsPage_errorRefreshingTree;
    /****/
    public static String ImportRPMsPage_errorResourceChanged;

    // ImportRPMsPage$ImportButtonListener
    /****/
    public static String ImportButtonListener_error;

    // ImportRPMsPage$RemoveButtonListener
    /****/
    public static String RemoveButtonListener_error;

    // MetadataPage
    /****/
    public static String MetadataPage_title;
    /****/
    public static String MetadataPage_formHeaderText;
    /****/
    public static String MetadataPage_sectionTitleRevision;
    /****/
    public static String MetadataPage_sectionInstructionRevision;
    /****/
    public static String MetadataPage_labelRevision;
    /****/
    public static String MetadataPage_sectionTitleTags;
    /****/
    public static String MetadataPage_sectionInstructionTags;
    /****/
    public static String MetadataPage_labelTags;
    /****/
    public static String MetadataPage_buttonAddTag;
    /****/
    public static String MetadataPage_buttonEditTag;
    /****/
    public static String MetadataPage_buttonRemoveTag;
    /****/
    public static String MetadataPage_errorSavingPreferences;

    // CreaterepoResourceChangeListener
    /****/
    public static String CreaterepoResourceChangeListener_errorGettingResource;

    // CreaterepoPreferencePage
    /****/
    public static String CreaterepoPreferencePage_description;
    /****/
    public static String CreaterepoPreferencePage_generalGroupLabel;
    /****/
    public static String CreaterepoPreferencePage_booleanChecksumName;
    /****/
    public static String CreaterepoPreferencePage_booleanGenerateSQLDB;
    /****/
    public static String CreaterepoPreferencePage_booleanIgnoreSymlinks;
    /****/
    public static String CreaterepoPreferencePage_booleanPrettyXML;
    /****/
    public static String CreaterepoPreferencePage_numWorkers;
    /****/
    public static String CreaterepoPreferencePage_updateGroupLabel;
    /****/
    public static String CreaterepoPreferencePage_booleanCheckTS;
    /****/
    public static String CreaterepoPreferencePage_checkTSNote;
    /****/
    public static String CreaterepoPreferencePage_changelogGroupLabel;
    /****/
    public static String CreaterepoPreferencePage_numChangelogLimit;
    /****/
    public static String CreaterepoPreferencePage_checksumGroupLabel;
    /****/
    public static String CreaterepoPreferencePage_compressionGroupLabel;

    // CreaterepoGeneralPropertyPage
    /****/
    public static String CreaterepoGeneralPropertyPage_projectSettings;
    /****/
    public static String CreaterepoGeneralPropertyPage_workspaceSettings;

    // CreaterepoDeltaPropertyPage
    /****/
    public static String CreaterepoDeltaPropertyPage_description;
    /****/
    public static String CreaterepoDeltaPropertyPage_groupLabel;
    /****/
    public static String CreaterepoDeltaPropertyPage_groupDirectoryLabel;
    /****/
    public static String CreaterepoDeltaPropertyPage_booleanEnableLabel;
    /****/
    public static String CreaterepoDeltaPropertyPage_maxNumberOfDeltas;
    /****/
    public static String CreaterepoDeltaPropertyPage_maxDeltaSize;
    /****/
    public static String CreaterepoDeltaPropertyPage_errorInvalidText;
    /****/
    public static String CreaterepoDeltaPropertyPage_directoryDescription;
    /****/
    public static String CreaterepoDeltaPropertyPage_directoryDialogLabel;

    // ImportRPMDropListener
    /****/
    public static String ImportRPMDropListener_errorCopyingFileToProject;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

}
