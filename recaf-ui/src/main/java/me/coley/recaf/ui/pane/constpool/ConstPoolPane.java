package me.coley.recaf.ui.pane.constpool;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.cafedude.Constants;
import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.constant.*;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.ColumnPane;
import me.coley.recaf.ui.pane.DockingWrapperPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.HashMap;

import static me.coley.recaf.ui.util.Icons.getClassIcon;
import static me.coley.recaf.ui.util.Icons.getIconView;

public class ConstPoolPane extends BorderPane implements ClassRepresentation {
    private final Logger logger = Logging.get(ConstPoolPane.class);
    private final ListView<ConstPoolEntry> list = new ListView<>();
    private ConstPool constPool;
    private ClassInfo classInfo;

    /**
     * New outline panel.
     */
    public ConstPoolPane() {
        list.setCellFactory(param -> new ConstPoolCell());
        setCenter(list);
        //setBottom(createBottomBar());
    }

    @Override
    public void onUpdate(CommonClassInfo newValue) {
        classInfo = (ClassInfo)newValue;

        ClassFileReader classFileReader = new ClassFileReader();
        ClassFile classFile;
        try {
            classFile = classFileReader.read(classInfo.getValue());
        } catch (InvalidClassException e) {
            logger.error("Failed to read class file {}", classInfo.getName());
            return;
        }

        constPool = classFile.getPool();
        list.getItems().addAll(constPool);
    }

    @Override
    public CommonClassInfo getCurrentClassInfo() {
        return classInfo;
    }

    @Override
    public boolean supportsMemberSelection() {
        return true;
    }

    @Override
    public boolean isMemberSelectionReady() {
        return false;
    }

    @Override
    public void selectMember(MemberInfo memberInfo) {
    }

    @Override
    public SaveResult save() {
        throw new UnsupportedOperationException("Outline pane does not support modification");
    }

    @Override
    public boolean supportsEditing() {
        // Outline is just for show. Save keybind does nothing here.
        return false;
    }

    @Override
    public Node getNodeRepresentation() {
        return this;
    }

    /**
     * @return Box containing tree display options.
     */
    private Node createBottomBar() {
        //HBox box = new HBox();
        /*TextField filter = new TextField();
        filter.setPromptText("Filter: Type/content...");
        filter.getStyleClass().add("filter-field");
        box.getChildren().add(filter);*/
        return null;
    }

    private static class ConstPoolEditPane extends BorderPane {
        private final ColumnPane columns = new ColumnPane();

        ConstPoolEditPane() {
            DockingWrapperPane wrapper = DockingWrapperPane.builder()
                    //.key("menu.search." + type)
                    .title(Lang.getBinding("search.text"))
                    .content(columns)
                    .size(600, 300)
                    .build();

            setCenter(wrapper);
        }
    }

    private class ConstPoolEditWindow extends GenericWindow {
        public ConstPoolEditWindow(ConstPoolCell cell, CellTemplate template, ConstPoolEditPane content) {
            super(content);

            setTitle("Edit constant pool entry");

            content.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    this.close();
                }

                if (e.getCode() == KeyCode.ENTER) {
                    template.onSave(content, cell);

                    this.close();
                }
            });
        }
    }

    abstract static class CellTemplate {
        private final String name; // translationKey?
        private final String graphicPath;

        CellTemplate(String name, String graphicPath) {
            this.name = name;
            this.graphicPath = graphicPath;
        }

        public String getName() {
            return name;
        }

        public String getGraphicPath() {
            return graphicPath;
        }

        public VBox create(Cell<?> cell, Node icon, String info) {
            VBox content = new VBox();
            HBox nameHbox = new HBox();
            HBox infoHbox = new HBox();
            Label nameLabel = new Label(getName());
            Label infoLabel = new Label(info);

            nameHbox.setSpacing(3);
            infoHbox.setPadding(new Insets(0, 0, 0, 19));
            nameHbox.getChildren().addAll(icon == null ? getIconView(getGraphicPath()) : icon, nameLabel);
            infoHbox.getChildren().add(infoLabel);

            content.getChildren().addAll(nameHbox, infoHbox);
            cell.setGraphic(content);

            return content;
        }

        public VBox create(ConstPoolCell cell, String info) {
            return create(cell, null, info);
        }

        abstract void applyToCell(ConstPoolCell cell);
        abstract void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell);
        abstract void onSave(ConstPoolEditPane pane, ConstPoolCell cell);
    }

    private final class CpUtf8CellTemplate extends CellTemplate {
        CpUtf8CellTemplate() {
            super("UTF-8", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpUtf8 item = (CpUtf8) cell.getItem();
            create(cell, String.format("\"%s\"", item.getText()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {
            CpUtf8 item = (CpUtf8) cell.getItem();

            Label textLabel = new Label("Text");
            TextField textField = new TextField(item.getText());
            pane.columns.add(textLabel, textField);
        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {
           /* CpUtf8 item = (CpUtf8) cell.getItem();
            TextField textField = (TextField)pane.columns.getRight(0);
            item.setText(textField.getText());

            constPool.*/
        }
    }

    private final class CpIntegerCellTemplate extends CellTemplate {
        CpIntegerCellTemplate() {
            super("Integer", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpInt item = (CpInt) cell.getItem();
            create(cell, String.valueOf(item.getValue()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpFloatCellTemplate extends CellTemplate {
        CpFloatCellTemplate() {
            super("Float", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpFloat item = (CpFloat)cell.getItem();
            create(cell, String.valueOf(item.getValue()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpLongCellTemplate extends CellTemplate {
        CpLongCellTemplate() {
            super("Long", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpLong item = (CpLong)cell.getItem();
            create(cell, String.valueOf(item.getValue()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpDoubleCellTemplate extends CellTemplate {
        CpDoubleCellTemplate() {
            super("Double", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpDouble item = (CpDouble)cell.getItem();
            create(cell, String.valueOf(item.getValue()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpClassCellTemplate extends CellTemplate {
        CpClassCellTemplate() {
            super("Class", Icons.CLASS);
        }

        @Override
        public void applyToCell(ConstPoolCell cell) {
            CpClass item = (CpClass)cell.getItem();
            String name = constPool.getUtf(item.getIndex());
            ClassInfo info = RecafUI.getController().getWorkspace().getResources().getClass(name);

            // Set class icon to be more specific
            // e.g private, protected etc
            create(cell, info != null ? getClassIcon(info) : null, name);
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpStringCellTemplate extends CellTemplate {
        CpStringCellTemplate() {
            super("String", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpString item = (CpString)cell.getItem();
            String name = constPool.getUtf(item.getIndex());
            create(cell, String.format("\"%s\"", name));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private class ConstRefCellTemplate extends CellTemplate {
        ConstRefCellTemplate(String name, String graphicPath) {
            super(name, graphicPath);
        }

        public void applyToCell(ConstRef item, ConstPoolCell cell) {
            CpClass clazz = (CpClass)constPool.get(item.getClassIndex());
            CpNameType nameType = (CpNameType)constPool.get(item.getNameTypeIndex());

            VBox content = create(cell, String.format("Class: %s",  constPool.getUtf(clazz.getIndex())));

            HBox nameHBox = new HBox();
            nameHBox.setPadding(new Insets(0, 0, 0, 19));
            Label nameLabel = new Label(String.format("Name: %s", constPool.getUtf(nameType.getNameIndex())));
            nameHBox.getChildren().add(nameLabel);

            HBox typeHBox = new HBox();
            typeHBox.setPadding(new Insets(0, 0, 0, 19));
            Label typeLabel = new Label(String.format("Type: %s", constPool.getUtf(nameType.getTypeIndex())));
            typeHBox.getChildren().add(typeLabel);

            content.getChildren().addAll(nameHBox, typeHBox);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            ConstRef item = (ConstRef)cell.getItem();
            applyToCell(item, cell);
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpFieldRefCellTemplate extends ConstRefCellTemplate {
        CpFieldRefCellTemplate() {
            super("Field reference", Icons.QUOTE);
        }
    }

    private final class CpMethodRefCellTemplate extends ConstRefCellTemplate {
        CpMethodRefCellTemplate() {
            super("Method reference", Icons.QUOTE);
        }
    }

    private final class CpInterfaceMethodRefCellTemplate extends ConstRefCellTemplate {
        CpInterfaceMethodRefCellTemplate() {
            super("Interface method reference", Icons.QUOTE);
        }
    }

    private final class CpNameTypeCellTemplate extends ConstRefCellTemplate {
        CpNameTypeCellTemplate() {
            super("Name and type", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpNameType item = (CpNameType)cell.getItem();

            VBox content = create(cell, String.format("Name: %s", constPool.getUtf(item.getNameIndex())));

            HBox typeHBox = new HBox();
            typeHBox.setPadding(new Insets(0, 0, 0, 19));
            Label typeLabel = new Label(String.format("Type: %s", constPool.getUtf(item.getTypeIndex())));
            typeHBox.getChildren().add(typeLabel);

            content.getChildren().addAll(typeHBox);
        }
    }

    private static final HashMap<Byte, String> methodHandleKinds = new HashMap<>() {
        {
            put((byte) 1, "REF_getField");
            put((byte) 2, "REF_getStatic");
            put((byte) 3, "REF_putField");
            put((byte) 4, "REF_putStatic");
            put((byte) 5, "REF_invokeVirtual");
            put((byte) 6, "REF_invokeStatic");
            put((byte) 7, "REF_invokeSpecial");
            put((byte) 8, "REF_newInvokeSpecial");
            put((byte) 9, "REF_invokeInterface");
        }
    };

    private final class CpMethodHandleCellTemplate extends ConstRefCellTemplate {
        CpMethodHandleCellTemplate() {
            super("Method handle", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpMethodHandle item = (CpMethodHandle) cell.getItem();
            ConstRef ref = (ConstRef)constPool.get(item.getReferenceIndex());
            super.applyToCell(ref, cell);

            String kind = methodHandleKinds.getOrDefault(item.getKind(), "Unknown kind");
            HBox kindHBox = new HBox();
            kindHBox.setPadding(new Insets(0, 0, 0, 19));
            Label kindLabel = new Label(String.format("Kind: %s", kind));
            kindHBox.getChildren().add(kindLabel);

            VBox content = (VBox)cell.getGraphic();
            content.getChildren().add(kindHBox);
        }
    }

    private final class CpMethodTypeCellTemplate extends CellTemplate {
        CpMethodTypeCellTemplate() {
            super("Method type", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpMethodType item = (CpMethodType) cell.getItem();
            create(cell, constPool.getUtf(item.getIndex()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpGenericCellTemplate extends CellTemplate {
        CpGenericCellTemplate() {
            super("Not implemented", Icons.HELP);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            create(cell, "None");
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private class DynamicCellTemplate extends CellTemplate {
        DynamicCellTemplate(String name, String graphicPath) {
            super(name, graphicPath);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            ConstDynamic item = (ConstDynamic) cell.getItem();
            CpNameType nameType = (CpNameType)constPool.get(item.getNameTypeIndex());

            VBox content = create(cell, String.format("Bootstrap method index: %d", item.getBsmIndex()));

            HBox nameHBox = new HBox();
            nameHBox.setPadding(new Insets(0, 0, 0, 19));
            Label nameLabel = new Label(String.format("Name: %s", constPool.getUtf(nameType.getNameIndex())));
            nameHBox.getChildren().add(nameLabel);

            HBox typeHBox = new HBox();
            typeHBox.setPadding(new Insets(0, 0, 0, 19));
            Label typeLabel = new Label(String.format("Type: %s", constPool.getUtf(nameType.getTypeIndex())));
            typeHBox.getChildren().add(typeLabel);

            content.getChildren().addAll(nameHBox, typeHBox);
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpDynamicCellTemplate extends DynamicCellTemplate {
        CpDynamicCellTemplate() {
            super("Dynamic", Icons.QUOTE);
        }
    }

    private final class CpInvokeDynamicCellTemplate extends DynamicCellTemplate {
        CpInvokeDynamicCellTemplate() {
            super("Invoke dynamic", Icons.QUOTE);
        }
    }

    private final class CpModuleCellTemplate extends CellTemplate {
        CpModuleCellTemplate() {
            super("Module", Icons.QUOTE);
        }


        @Override
        void applyToCell(ConstPoolCell cell) {
            CpModule item = (CpModule)cell.getItem();
            create(cell, constPool.getUtf(item.getIndex()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final class CpPackageCellTemplate extends CellTemplate {
        CpPackageCellTemplate() {
            super("Package", Icons.QUOTE);
        }

        @Override
        void applyToCell(ConstPoolCell cell) {
            CpPackage item = (CpPackage)cell.getItem();
            create(cell, constPool.getUtf(item.getIndex()));
        }

        @Override
        void setEditorContent(ConstPoolEditPane pane, ConstPoolCell cell) {

        }

        @Override
        void onSave(ConstPoolEditPane pane, ConstPoolCell cell) {

        }
    }

    private final HashMap<Integer, CellTemplate> cellTemplates = new HashMap<>() {
        {
            put(Constants.ConstantPool.UTF8, new CpUtf8CellTemplate());
            put(Constants.ConstantPool.INTEGER, new CpIntegerCellTemplate());
            put(Constants.ConstantPool.FLOAT, new CpFloatCellTemplate());
            put(Constants.ConstantPool.LONG, new CpLongCellTemplate());
            put(Constants.ConstantPool.DOUBLE, new CpDoubleCellTemplate());
            put(Constants.ConstantPool.CLASS, new CpClassCellTemplate());
            put(Constants.ConstantPool.STRING, new CpStringCellTemplate());
            put(Constants.ConstantPool.FIELD_REF, new CpFieldRefCellTemplate());
            put(Constants.ConstantPool.METHOD_REF, new CpMethodRefCellTemplate());
            put(Constants.ConstantPool.INTERFACE_METHOD_REF, new CpInterfaceMethodRefCellTemplate());
            put(Constants.ConstantPool.NAME_TYPE, new CpNameTypeCellTemplate());
            put(Constants.ConstantPool.METHOD_HANDLE, new CpMethodHandleCellTemplate());
            put(Constants.ConstantPool.METHOD_TYPE, new CpMethodTypeCellTemplate());
            put(Constants.ConstantPool.DYNAMIC, new CpDynamicCellTemplate());
            put(Constants.ConstantPool.INVOKE_DYNAMIC, new CpInvokeDynamicCellTemplate());
            put(Constants.ConstantPool.MODULE, new CpModuleCellTemplate());
            put(Constants.ConstantPool.PACKAGE, new CpPackageCellTemplate());
        }
    };

    private class ConstPoolCell extends ListCell<ConstPoolEntry> {
        @Override
        protected void updateItem(ConstPoolEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setText(null);
                setGraphic(null);
            } else {
                CellTemplate template = cellTemplates.getOrDefault(item.getTag(), new CpGenericCellTemplate());
                template.applyToCell(this);

                // Open the editor when the cell is double-clicked
                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        ConstPoolEditPane pane = new ConstPoolEditPane();
                        template.setEditorContent(pane, this);
                        ConstPoolEditWindow window = new ConstPoolEditWindow(this, template, pane);
                        window.show();
                    }
                });
            }
        }
    }
}
