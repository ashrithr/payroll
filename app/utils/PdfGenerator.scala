package utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.tool.xml.XMLWorkerHelper
import com.typesafe.config.Config

object PdfGenerator {

  def generate(content: ByteArrayInputStream, config: Config): ByteArrayOutputStream = {
    val document = new Document
    val outputStream = new ByteArrayOutputStream()
    val footer = XMLWorkerHelper.parseToElementList(
      s"""
        |<table width="100%" border="0">
        | <tr>
        |   <td align="center">
        |     <p style="margin: 20px 0;color: #818a91;font-size: 80%;">
        |       ${config.getString("company.name")}
        |       <br/>
        |       ${config.getString("company.address.street")}, ${config.getString("company.address.city")} ${config.getString("company.address.state")} ${config.getString("company.address.zip")} | ${config.getString("company.contact.email")} | ${config.getString("company.contact.phone")}
        |     </p>
        |   </td>
        | </tr>
        |</table>
        |
      """.stripMargin,
      null
    )
    val writer = PdfWriter.getInstance(document, outputStream)
    val footerEvent = new PdfUtils.Footer(footer)
    writer.setPageEvent(footerEvent)
    document.open()
    XMLWorkerHelper.getInstance().parseXHtml(writer, document, content)
    document.close()
    outputStream.close()
    outputStream
  }

}
