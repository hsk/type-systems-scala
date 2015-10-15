package trans
import java.awt.datatransfer._
import java.io._

object main extends App with ClipboardOwner{

  val clip = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
  override def lostOwnership(aClipboard:Clipboard, aContents:Transferable){}

  def getclip():String = {
    try{
      clip.getData(DataFlavor.stringFlavor).asInstanceOf[String]
    } catch {
      case _:Throwable => ""
    }
  }

  def setclip(str:String) {
    val ss = new StringSelection(str)
    clip.setContents(ss, ss);
  }

  def keywait():Boolean = {
    val bufferedReader = new BufferedReader(new InputStreamReader(System.in))
    if (bufferedReader.ready()) {
      bufferedReader.readLine()
      false
    } else {
      true
    }
  }

  def exec(strs:Array[String]) {
    val p = Runtime.getRuntime().exec(strs)
    p.getErrorStream().close()
    p.getInputStream().close()
    p.getOutputStream().close()
    p.waitFor()
    p.destroy()
  }


  def main() {
    def getlog():String = {
      var log=""
      var prev = getclip()
      while(keywait()) {
        val data = getclip()
        if (prev != data) {
          println(data)
          log += data + "\n"
        }
        prev = data
        Thread.sleep(100)
      }
      log
    }
    def openbrowser(log:String) {
      val log2 = java.net.URLEncoder.encode(log, "UTF-8")
      exec(Array("open", "https://translate.google.co.jp/#en/ja/"+log2))
    }
    def waitcopy() {
      println("prease copy translate words...")
      val prev = getclip()
      while(getclip()==prev){
        Thread.sleep(100)
      }
    }
    def maketable(log:String):String = {
      val data = getclip().split('\n')
      val log3 = log.trim().split('\n')

      var out = ""

      out += "<sup><sub>\n"
      //out += "|English|日本語|\n"
      //out += "| --- | --- |\n"
      data.toList.zipWithIndex.foreach {
        case (v,i) =>
          //out += "|"+log3(i)+"|"+v+"|\n"
          out += log3(i)+" "+v+"\n"
      }
      out += "</sub></sup>\n"
      out
    }

    val log = getlog()
    openbrowser(log)
    waitcopy()
    val table = maketable(log)
    print(table)
    setclip(table)
    System.exit(0)
  }
  main()
}

