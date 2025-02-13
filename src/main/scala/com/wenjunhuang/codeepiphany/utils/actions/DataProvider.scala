package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.{DataKey, DataProvider}

/** A bridge between old DataProvider and new UiDataProvider */
trait UiDataProvider extends DataProvider {
  def uiDataSnapshot(sink: DataSink): Unit

  private var myDataSink: Option[DataSink] = None
  override def getData(dataId: String): AnyRef = synchronized {
    myDataSink match
      case Some(sink) => sink.getData(dataId)
      case None =>
        val sink = DataSink()
        uiDataSnapshot(sink)
        myDataSink = Some(sink)
        sink.getData(dataId)
  }
}

class DataSink {
  private var myDatas = Map.empty[String, () => AnyRef]

  def set[T <: AnyRef](key: DataKey[T], data: T): Unit = {
    myDatas = myDatas.updated(key.getName, () => data)
  }

  /** Put the [PlatformCoreDataKeys.BGT_DATA_PROVIDER] lambda in the sink
    */
  def `lazy`[T <: AnyRef](key: DataKey[T], data: () => T): Unit =
    myDatas = myDatas.updated(key.getName, data)

  def getData(dataId: String): AnyRef = myDatas.get(dataId).map(_.apply()).orNull

}
