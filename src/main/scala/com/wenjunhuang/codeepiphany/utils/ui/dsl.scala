package com.wenjunhuang.codeepiphany.utils.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.{ ActionPlaces, AnAction }
import com.intellij.openapi.application.{ ApplicationManager, ModalityState }
import com.intellij.openapi.fileChooser.{
  FileChooserDescriptor,
  FileChooserDescriptorFactory
}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{
  ComboBox,
  DialogPanel,
  TextFieldWithBrowseButton
}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.{
  CellKt,
  ComponentPredicate,
  ComponentPredicateKt,
  RowKt
}
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.ui.ThreeStateCheckBox
import kotlin.ranges.{ ClosedRange, IntRange, RangesKt }

import java.awt.{ Color, Font }
import java.awt.event.ActionEvent
import java.lang
import javax.swing.*
import scala.annotation.targetName
import scala.collection.immutable.NumericRange.Inclusive as NumInclusive
import scala.collection.immutable.Range.Inclusive
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import scala.reflect.{ classTag, ClassTag }
import com.intellij.ui.dsl.builder.{ AlignX, RightGap, RowLayout, TextFieldKt }

import com.intellij.openapi.ui.Splitter

/** A Scala DSL for creating UI components in IntelliJ IDEA, serving as a simple
  * wrapper around the Kotlin UI DSL.
  */
object dsl {

  private val intellijUIContext: ExecutionContext =
    ExecutionContext.fromExecutor { runnable =>
      ApplicationManager.getApplication.invokeLater(
        runnable,
        ModalityState.any()
      )
    }
  given any2KtUnit: Conversion[Any, kotlin.Unit] = _ => kotlin.Unit.INSTANCE

  given abstractButton2ComponentPredicate
    : Conversion[AbstractButton, ComponentPredicate] = { button =>
    ComponentPredicateKt.getSelected(button)
  }

  given cell2ComponentPredicate[T <: JComponent](using
    conv: Conversion[T, ComponentPredicate]
  ): Conversion[Cell[T], ComponentPredicate] = { cell =>
    conv(cell.getComponent)
  }

  extension [T](a: T) {
    def void: Unit = ()
  }

  extension (component: JComponent) {
    def bold(isBold: Boolean): Unit =
      component.setFont(
        component.getFont.deriveFont(if isBold then Font.BOLD else Font.PLAIN)
      )
  }

  extension [T <: JComponent](cell: Cell[T]) {
    def comment(comment: String): Cell[T] =
      cell.comment(
        comment,
        UtilsKt.DEFAULT_COMMENT_WIDTH,
        HyperlinkEventAction.HTML_HYPERLINK_INSTANCE
      )

    def comment(comment: String, maxLineLength: Int): Cell[T] =
      cell.comment(
        comment,
        maxLineLength,
        HyperlinkEventAction.HTML_HYPERLINK_INSTANCE
      )

    def comment(comment: String, action: HyperlinkEventAction): Cell[T] =
      cell.comment(comment, UtilsKt.DEFAULT_COMMENT_WIDTH, action)

    def label(label: String): Cell[T] =
      cell.label(label, LabelPosition.LEFT)
      
      

  }

  extension (group: ButtonsGroup) {
    def bind[T <: AnyRef: ClassTag](
      getter: () => T,
      setter: T => Unit
    ): ButtonsGroup =
      group.bind(
        MutablePropertyKt.MutableProperty[T](() => getter(), v => setter(v)),
        classTag[T].runtimeClass.asInstanceOf[Class[T]]
      )
  }

  extension (row: Row) {
    def rowComment(comment: String): Row = row.rowComment(
      comment,
      UtilsKt.DEFAULT_COMMENT_WIDTH,
      HyperlinkEventAction.HTML_HYPERLINK_INSTANCE
    )

    def comment(comment: String)(
      action: HyperlinkEventAction
    ): Cell[JEditorPane] =
      row.comment(comment, UtilsKt.DEFAULT_COMMENT_WIDTH, action)

    def comment(comment: String): Cell[JEditorPane] =
      row.comment(
        comment,
        UtilsKt.DEFAULT_COMMENT_WIDTH,
        HyperlinkEventAction.HTML_HYPERLINK_INSTANCE
      )
  }

  extension (slider: Cell[JSlider]) {
    def labelTable(labelTable: Map[Int, JLabel]): Cell[JSlider] =
      SliderKt.labelTable(
        slider,
        labelTable.map { case (k, v) => int2Integer(k) -> v }.asJava
      )
  }

  extension (cell: Cell[JBTextField]) {
    def text(text: String): Cell[JBTextField] =
      TextFieldKt.text(cell, text)

    @targetName("tfColumns")
    def columns(cols: Int): Cell[JBTextField] =
      TextFieldKt.columns(cell, cols)

    @targetName("tfBindText")
    def bindText(
      getter: () => String,
      setter: String => Unit
    ): Cell[JBTextField] =
      TextFieldKt.bindText(cell, () => getter(), v => setter(v))
  }

  extension (textArea: Cell[JBTextArea]) {
    def rows(row: Int): Cell[JBTextArea] =
      TextAreaKt.rows(textArea, row)

    @targetName("columnsTextArea")
    def columns(col: Int): Cell[JBTextArea] =
      TextAreaKt.columns(textArea, col)
  }

  extension [T <: AbstractButton](button: Cell[T]) {
    def selected: ComponentPredicate =
      ComponentPredicateKt.getSelected(button.getComponent)

    def selected(s: Boolean): Cell[T] =
      ButtonKt.selected(button, s)
  }

  def dialogPanel(f: Panel ?=> Unit): DialogPanel = BuilderKt.panel { panel =>
    f(using panel); panel
  }

  def group()(f: Panel ?=> Unit)(using panel: Panel): Row =
    panel.group(null: String, true, group => f(using group))

  def group(indent: Boolean)(f: Panel ?=> Unit)(using panel: Panel): Row =
    panel.group(null: String, indent, group => f(using group))

  def group(title: String)(f: Panel ?=> Unit)(using panel: Panel): Row =
    panel.group(title, true, group => f(using group))

  def indent(using panel: Panel)(f: Panel ?=> Unit): RowsRange =
    panel.indent(p => f(using p))

  def twoColumnsRow(first: Row ?=> Unit, second: Row ?=> Unit)(using
    panel: Panel
  ): Row =
    panel.twoColumnsRow(row => first(using row), row => second(using row))

  def buttonsGroup(title: String = null, indent: Boolean = false)(
    f: Panel ?=> Unit
  )(using panel: Panel): ButtonsGroup =
    panel.buttonsGroup(title, indent, panel => f(using panel))

  def row(label: String)(f: Row ?=> Unit)(using panel: Panel): Row =
    panel.row(label, row => f(using row))

  def row(jlabel: JLabel = null)(f: Row ?=> Unit)(using panel: Panel): Row =
    panel.row(jlabel, row => f(using row))

  def textField()(using row: Row): Cell[JBTextField] = row.textField()

  def textField(text: String)(using row: Row): Cell[JBTextField] =
    TextFieldKt.text(row.textField(), text)

  def checkBox(label: String)(using row: Row): Cell[JBCheckBox] =
    row.checkBox(label)

  def button(label: String)(using row: Row): Cell[JButton] =
    row.button(label, e => ())

  def button(label: String, action: ActionEvent => Unit)(using
    row: Row
  ): Cell[JButton] = row.button(label, e => action(e))

  def button(label: String, action: () => Unit)(using row: Row): Cell[JButton] =
    row.button(label, e => action())

  def actionButton(action: AnAction)(using row: Row): Cell[ActionButton] =
    ExtensionsKt.actionButton(row, action, ActionPlaces.UNKNOWN)

  def actionsButton(actions: AnAction*)(using row: Row): Cell[ActionButton] =
    ExtensionsKt.actionsButton(
      row,
      actions.toArray,
      ActionPlaces.UNKNOWN,
      AllIcons.General.GearPlain
    )

  def segmentedButton[T](list: List[T])(
    f: (pre: SegmentedButton.ItemPresentation, v: T) => Unit
  )(using row: Row): SegmentedButton[T] =
    row.segmentedButton(list.asJava, (presentation, v) => f(presentation, v))

  def tabbedPaneHeader(list: List[String])(using row: Row): Cell[JBTabbedPane] =
    ExtensionsKt.tabbedPaneHeader(row, list.asJava)

  def label(text: String)(using row: Row): Cell[JLabel] = row.label(text)

  def text(text: String)(using row: Row): Cell[JEditorPane] = row.text(
    text,
    UtilsKt.MAX_LINE_LENGTH_WORD_WRAP,
    HyperlinkEventAction.HTML_HYPERLINK_INSTANCE
  )

  def threeStateCheckBox(label: String)(using
    row: Row
  ): Cell[ThreeStateCheckBox] = row.threeStateCheckBox(label)

  def comment(comment: String)(using row: Row): Cell[JEditorPane] =
    row.comment(
      comment,
      UtilsKt.DEFAULT_COMMENT_WIDTH,
      HyperlinkEventAction.HTML_HYPERLINK_INSTANCE
    )

  def comment(comment: String, maxLineLength: Int)(using
    row: Row
  ): Cell[JEditorPane] =
    row.comment(
      comment,
      UtilsKt.DEFAULT_COMMENT_WIDTH,
      HyperlinkEventAction.HTML_HYPERLINK_INSTANCE
    )

  def comment(comment: String, action: HyperlinkEventAction)(using
    row: Row
  ): Cell[JEditorPane] =
    row.comment(comment, UtilsKt.DEFAULT_COMMENT_WIDTH, action)

  def comment(
    comment: String,
    maxLineLength: Int,
    action: HyperlinkEventAction
  )(using row: Row): Cell[JEditorPane] =
    row.comment(comment, maxLineLength, action)

  def radioButton[T](label: String, value: T)(using
    row: Row
  ): Cell[JBRadioButton] =
    row.radioButton(label, value)

  def link(label: String)(using row: Row): Cell[ActionLink] =
    row.link(label, e => ())
  def link(label: String)(f: ActionEvent => Unit)(using
    row: Row
  ): Cell[ActionLink] =
    row.link(label, e => f(e))

  def browserLink(label: String, url: String)(using
    row: Row
  ): Cell[BrowserLink] =
    row.browserLink(label, url)

  def dropDownLink[T](item: T, items: List[T])(using
    row: Row
  ): Cell[DropDownLink[T]] =
    row.dropDownLink(item, items.asJava)

  def icon(icon: Icon)(using row: Row): Cell[JLabel] = row.icon(icon)

  def contextHelp(description: String, title: String)(using
    row: Row
  ): Cell[JLabel] =
    row.contextHelp(description, title)

  def passwordField()(using row: Row): Cell[JBPasswordField] =
    row.passwordField()

  def textFieldWithBrowseButton(
    title: String,
    project: Project = null,
    descriptor: FileChooserDescriptor =
      FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
    fileChosen: kotlin.jvm.functions.Function1[VirtualFile, String] = null
  )(using row: Row): Cell[TextFieldWithBrowseButton] =
    row.textFieldWithBrowseButton(title, project, descriptor, fileChosen)

  def expandableTextField(
    parser: String => List[String] = null,
    joiner: List[String] => String = null
  )(using row: Row): Cell[ExpandableTextField] =
    row.expandableTextField(
      if parser == null then ParametersListUtil.DEFAULT_LINE_PARSER
      else { str => parser(str).asJava },
      if joiner == null then ParametersListUtil.COLON_LINE_JOINER
      else { list => joiner(list.asScala.toList) }
    )

  def intTextField(range: Inclusive)(using row: Row): Cell[JBTextField] =
    row.intTextField(IntRange(range.start, range.end), range.step)

  def spinner(range: Inclusive)(using row: Row): Cell[JBIntSpinner] =
    row.spinner(IntRange(range.start, range.end), range.step)

  def spinner(range: NumInclusive[BigDecimal])(using row: Row): Cell[JSpinner] =
    row.spinner(
      new ClosedRange[lang.Double] {
        override def getEndInclusive: lang.Double =
          double2Double(range.end.toDouble)

        override def getStart: lang.Double =
          double2Double(range.start.toDouble)

        override def contains(t: lang.Double): Boolean =
          range.start <= BigDecimal(t) && BigDecimal(t) <= range.end

        override def isEmpty: Boolean = range.start > range.end
      },
      double2Double(range.step.toDouble)
    )

  def slider(min: Int, max: Int, minorTickSpacing: Int, majorTickSpacing: Int)(
    using row: Row
  ): Cell[JSlider] =
    row.slider(min, max, minorTickSpacing, majorTickSpacing)

  def textArea()(using row: Row): Cell[JBTextArea] = row.textArea()

  def comboBox[T](items: List[T], render: ListCellRenderer[T] = null)(using
    row: Row
  ): Cell[ComboBox[T]] =
    row.comboBox(items.asJava, render)

  def scrollCell[T <: JComponent](panel: T)(using row: Row): Cell[T] =
    row.scrollCell(panel)

  def cell()(using row: Row) = row.cell()

  def cell[T <: JComponent](component: T)(using row: Row): Cell[T] =
    row.cell(component)

  def panelPanel()(f: Panel ?=> Unit)(using p: Panel): Panel =
    p.panel(p => f(using p))

  def rowPanel()(f: Panel ?=> Unit)(using row: Row): Panel =
    row.panel(p => f(using p))

  def rowsRange()(f: Panel ?=> Unit)(using p: Panel): RowsRange =
    p.rowsRange(p => f(using p))

  def groupRowsRange(
    title: String = null,
    indent: Boolean = true,
    topGroupGap: Boolean = false,
    bottomGroupGap: Boolean = false
  )(f: Panel ?=> Unit)(using p: Panel) =
    p.groupRowsRange(
      title,
      indent,
      topGroupGap,
      bottomGroupGap,
      p => f(using p)
    )

  def collapsibleGroup(title: String, indent: Boolean = true)(
    f: Panel ?=> Unit
  )(using p: Panel): CollapsibleRow =
    p.collapsibleGroup(title, indent, p => f(using p))

  def separator(background: Color = null)(using p: Panel) =
    p.separator(background)

  def splitter(
    first: JComponent,
    second: JComponent,
    vertical: Boolean = false,
    proportion: Float = 0.5
  )(using row: Row) = {
    val splitter = Splitter(vertical, proportion)
    splitter.setFirstComponent(first)
    splitter.setSecondComponent(second)
    row.cell(splitter)
  }

  object constants {
    final val COLUMNS_MEDIUM = TextFieldKt.COLUMNS_MEDIUM
    final val CCOLUMNS_SHORT = TextFieldKt.COLUMNS_SHORT
    final val COLUMNS_TINY   = TextFieldKt.COLUMNS_TINY
    final val COLUMNS_LARGE  = TextFieldKt.COLUMNS_LARGE
  }
}
