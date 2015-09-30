package refined
/*
 Z3を呼び出すインターフェイス

*/
object Smt {
  import java.io._

  sealed trait answer
  case object Sat extends answer
  case object Unsat extends answer
  case object Unknown extends answer
  case class Error(a:String) extends answer

  sealed trait solver
  case object Standard extends solver
  case object NLSat extends solver
  case object Both extends solver

  val log_Z3_input = false
  var solver:solver = Both

  var info:Option[(Process,BufferedReader,PrintStream)] = None
  var log = ""

  def debuglog(msg:String) {
    log = log + msg + "\n"
    //println(msg)
  }
  var stack = 0

  def is_started():Boolean = {
    None != info
  }

  // TODO プロセス呼び出しをしてるコード持って来る
  // プロセスを止める
  def stop() {
    debuglog(";*stop")

    info match {
      case None => //println("Z3 not running")
      case Some((p, c_in, c_out)) =>
        p.getErrorStream().close()
        p.getInputStream().close()
        p.getOutputStream().close()
        try {
          p.waitFor() match {
            case 0 =>
            case exit_code => println("Z3 exited with exit code " + exit_code)
            //case Unix.WSIGNALED(signal) => println("Z3 was killed by a signal " + signal)
            //case Unix.WSTOPPED(signal) => println("Z3 was stopped by a signal " + signal)
          }
        } catch {
          case e:Throwable => println("Z3 exit exception"+e)
        }

        /*
        if (process_status != Unix.WEXITED(0) || log_Z3_input) {
          println("\n\nZ3 LOG\n")
          println(log)
        }*/
        if (stack != 0) println("\nERROR: STACK = " + stack + "\n" )
        info = None
    }
  }

  // 出力ストリーム取得
  def get_out_channel():PrintStream = {
    info match {
      case None => throw new Exception("Z3 not running")
      case Some((_, _, c_out)) => c_out
    }
  }

  // 入力ストリーム取得
  def get_in_channel():BufferedReader = {
    info match {
      case None => throw new Exception("Z3 not running")
      case Some((_, c_in, _)) => c_in
    }
  }

  // 入力ストリームから読み込み
  def read():answer = {
    val c_in = get_in_channel()
    while(!c_in.ready() ) {
      Thread.sleep(10)
    }

    val v = c_in.readLine()
    debuglog("; read data "+v)
    v match {
      case "unsat" => Unsat
      case "sat" => Sat
      case "unknown" => Unknown
      case null => read()
      case error => Error(error)
    }
  }

  // 出力ストリームへ書き出し
  def write(str: String) {
    val c_out = get_out_channel()
    c_out.println(str)
    c_out.flush()
    debuglog(str)
  }

  // 状態チェック
  def check_sat():answer = {
    solver match {
      case Standard =>
        write("(check-sat)")
        read()
      case NLSat =>
        write("(check-sat-using qfnra-nlsat)")
        read()
      case Both =>
        write("(check-sat)")
        read() match {
          case Unknown =>
            write("(check-sat-using qfnra-nlsat)")
            read()
          case answer => answer
        }
    }
  }

  // プロセス開始
  def start() {
    log = ""
    if (!is_started()) {
        val p = Runtime.getRuntime().exec("z3 -smt2 -in")
        val c_in = new BufferedReader(new InputStreamReader(p.getInputStream()))
        val c_out = new PrintStream(p.getOutputStream())
        info = Some((p, c_in, c_out))
      write("(set-option :global-decls false)")
      val stop1 = stop _
      Runtime.getRuntime().addShutdownHook(new Thread() {
        override def run() {
          stop1()
        }
      })
    }
  }

  // pushコマンド呼び出し
  def push() {
    write("(push)")
    stack += 1
  }

  // popコマンド呼び出し
  def pop() {
    write("(pop)\n")
    stack -= 1
  }

  // pushしたあと、処理を実行して、popする
  def push_pop[A](fn:()=>A):A = {
    push()
    val result = 
      try {
        fn()
      } catch {
      case e:Throwable =>
        pop()
        throw e
      }
    pop()
    result
  }

}
