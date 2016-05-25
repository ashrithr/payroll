package utils

import com.itextpdf.text.pdf.{ColumnText, PdfPageEventHelper, PdfWriter}
import com.itextpdf.text.{Document, Rectangle}
import com.itextpdf.tool.xml.ElementList
import scala.collection.JavaConversions._ // required for iterating over ElementList

object PdfUtils {

  class HeaderFooter(val header: ElementList, val footer: ElementList) extends PdfPageEventHelper {

    override def onEndPage(pdfWriter: PdfWriter, document: Document): Unit = {
      val ct: ColumnText = new ColumnText(pdfWriter.getDirectContent)
      ct.setSimpleColumn(new Rectangle(36, 832, 559, 810))
      for(e <- header) {
        ct.addElement(e)
      }
      ct.go()
      ct.setSimpleColumn(new Rectangle(36, 10, 559, 32))
      for(e <- footer) {
        ct.addElement(e)
      }
      ct.go()
      () // Explicitly return unit
    }

  }

  class Footer(val footer: ElementList) extends PdfPageEventHelper {

    override def onEndPage(pdfWriter: PdfWriter, document: Document): Unit = {
      val ct: ColumnText = new ColumnText(pdfWriter.getDirectContent)
      ct.setSimpleColumn(new Rectangle(36, 10, 559, 32))
      for(e <- footer) {
        ct.addElement(e)
      }
      ct.go()
      () // Explicitly return unit
    }

  }

}
