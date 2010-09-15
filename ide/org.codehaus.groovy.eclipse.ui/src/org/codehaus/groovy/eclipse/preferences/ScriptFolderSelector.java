/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.preferences;

import java.util.List;

import org.eclipse.jdt.groovy.core.Activator;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Dialog for creating and editing script folders in the workspace
 * 
 * @author andrew eisenberg
 * @created Sep 14, 2010
 */
public class ScriptFolderSelector {

    private static final ImageDescriptor DESCRIPTOR = JavaPluginImages.DESC_OBJS_INCLUSION_FILTER_ATTRIB;

    private static final int IDX_ADD = 0;

    private static final int IDX_EDIT = 1;

    private static final int IDX_REMOVE = 2;

    private static final String[] buttonLabels = { "Add", "Edit", "Remove" };

    private static class ScriptLabelProvider extends LabelProvider {

        private Image fElementImage;

        public ScriptLabelProvider(ImageDescriptor descriptor) {
            ImageDescriptorRegistry registry = JavaPlugin.getImageDescriptorRegistry();
            fElementImage = registry.get(descriptor);
        }

        @Override
        public Image getImage(Object element) {
            return fElementImage;
        }

        @Override
        public String getText(Object element) {
            return BasicElementLabels.getFilePattern((String) element);
        }

    }

    private class ScriptPatternAdapter implements IListAdapter, IDialogFieldListener {
        /**
         * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField,
         *      int)
         */
        public void customButtonPressed(ListDialogField field, int index) {
            doCustomButtonPressed(field, index);
        }

        /**
         * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
         */
        public void selectionChanged(ListDialogField field) {
            doSelectionChanged(field);
        }

        /**
         * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
         */
        public void doubleClicked(ListDialogField field) {
            doDoubleClicked(field);
        }

        /**
         * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
         */
        public void dialogFieldChanged(DialogField field) {}

    }

    private final Composite parent;

    private ListDialogField patternList;

    public ScriptFolderSelector(Composite parent) {
        this.parent = parent;
    }

    public ListDialogField createListContents() {

        Composite inner = new Composite(parent, SWT.NONE);
        inner.setFont(parent.getFont());
        GridLayout layout = new GridLayout();
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        layout.numColumns = 1;
        inner.setLayout(layout);
        inner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label label = new Label(inner, SWT.WRAP);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        label.setText("Groovy Script Folders:");

        // inner composite contains the dialog itself
        inner = new Composite(inner, SWT.NONE | SWT.BORDER);
        inner.setFont(parent.getFont());
        layout = new GridLayout();
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        layout.numColumns = 3;
        inner.setLayout(layout);
        inner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        ScriptPatternAdapter adapter = new ScriptPatternAdapter();

        patternList = new ListDialogField(adapter, buttonLabels, new ScriptLabelProvider(DESCRIPTOR));
        patternList.setDialogFieldListener(adapter);
        patternList.setLabelText("Groovy files that match these patterns are treated as scripts.  "
                + "They will not be compiled and will be copied as-is to the output folder.");
        patternList.setRemoveButtonIndex(IDX_REMOVE);
        patternList.enableButton(IDX_EDIT, false);
        patternList.doFillIntoGrid(inner, 3);
        Label l = patternList.getLabelControl(inner);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.widthHint = 200;
        l.setLayoutData(gd);

        resetElements();
        patternList.enableButton(IDX_ADD, true);
        patternList.setViewerComparator(new ViewerComparator());
        return patternList;
    }

    private List<String> findPatterns() {
        return Activator.getDefault().getListStringPreference(Activator.GROOVY_SCRIPT_FILTER,
                Activator.DEFAULT_GROOVY_SCRIPT_FILTER);
    }

    protected void doCustomButtonPressed(ListDialogField field, int index) {
        if (index == IDX_ADD) {
            addEntry(field);
        } else if (index == IDX_EDIT) {
            editEntry(field);
        }
    }

    protected void doSelectionChanged(ListDialogField field) {
        @SuppressWarnings("unchecked")
        List<String> selected = field.getSelectedElements();
        field.enableButton(IDX_EDIT, canEdit(selected));
    }

    protected void doDoubleClicked(ListDialogField field) {
        editEntry(field);
    }

    private boolean canEdit(List<String> selected) {
        return selected.size() == 1;
    }

    private void addEntry(ListDialogField field) {
        InputDialog dialog = createInputDialog("");
        if (dialog.open() == Window.OK) {
            field.addElement(dialog.getValue());
        }
    }

    private InputDialog createInputDialog(String initial) {
        InputDialog dialog = new InputDialog(
                parent.getShell(),
                "Add script folder",
                "Enter a pattern for denoting script files in Groovy projects. Allowed wildcards are '*', '?' and '**'. Examples: 'java/util/A*.java', 'java/util/', '**/Test*'.  All patterns are relative to the current project.",
                initial, null);
        return dialog;
    }

    private void editEntry(ListDialogField field) {
        @SuppressWarnings("unchecked")
        List<String> selElements = field.getSelectedElements();
        if (selElements.size() != 1) {
            return;
        }
        String entry = (String) selElements.get(0);
        InputDialog dialog = createInputDialog(entry);
        if (dialog.open() == Window.OK) {
            field.replaceElement(entry, dialog.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public void applyPreferences() {
        Activator.getDefault().setPreference(Activator.GROOVY_SCRIPT_FILTER, patternList.getElements());
    }

    public void restoreDefaultsPressed() {
        Activator.getDefault().setPreference(Activator.GROOVY_SCRIPT_FILTER,
                Activator.DEFAULT_GROOVY_SCRIPT_FILTER);

        resetElements();
    }

    /**
     *
     */
    private void resetElements() {
        List<String> elements = findPatterns();
        patternList.setElements(elements);
        patternList.selectFirstElement();
    }


}
