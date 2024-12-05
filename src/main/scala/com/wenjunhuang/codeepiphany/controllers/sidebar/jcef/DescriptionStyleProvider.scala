package com.wenjunhuang.codeepiphany.controllers.sidebar.jcef

trait DescriptionStyleProvider {

  /** The padding of the body element in the description view with css padding property order: top, right, bottom, left
    */
  def bodyPadding: Option[(Int, Int, Int, Int)]
}
