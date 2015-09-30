package refined

object Refine {
  import Expr._
  import Printing._
  import Smt._

  def error[A](msg:String):A = {
    throw new Exception(msg)
  }

  sealed trait Result
  case object Term extends Result
  case object Formula extends Result

  object LocalEnv {
    type env = Map[String, String]

    val empty : env = Map()

    def extend[A](name:String, ty:String, env:env):env = {
      if (env.contains(name))
        error("duplicate variable name \"" + name + "\"")
      else
        env + (name -> ty)
    }
    
    def lookup(name:String, env:env):String = {
      try {
        env(name)
      } catch {
      case _:Throwable =>
        name
      }
    }
  }

  object FnEnv {
    type env = Map[String,(LocalEnv.env, t_ty)]

    val empty : env = Map()
    def extend(name:String, local_env_and_ty:(LocalEnv.env, t_ty), env:env):env = {
      if (env.contains(name))
        error ("duplicate variable name \"" + name + "\"")
      else
        env + (name -> local_env_and_ty)
    }
    def lookup(name: String, env: env):(LocalEnv.env, t_ty) = env(name)
  }

  val builtins =
    Core.builtins.foldLeft(Set[String]()) {
    case (names, (name, ty_str)) =>
      if (!is_function_ty(Infer.Env.lookup(name, Core.env)))
        error("builtin symbol " + name + " must be a function")
      else
        names + name 
    }

  
  val uninterpreted =
    Core.uninterpreted.foldLeft(Set[String]()) {
      case (names, (name, ty_str)) =>
        if (!is_function_ty(Infer.Env.lookup(name, Core.env)))
          error ("uninterpreted symbol " + name + " must be a function")
        else
          names + name
    }

  val primitives =
    Core.primitives.foldLeft(Set[String]()){
      case(names, (name, ty_str)) => names + name
    }

  val translate_bool = (_:Boolean).toString

  val translate_int = (_:Int).toString

  def translate_ty[A](ty:Ty[A]):String =
    real_ty(ty) match {
      case TConst("int") => "Int"
      case TConst("bool") => "Bool"
      case TConst(_) | TApp(_,_) | TVar(_) => "Other"
      case TArrow(_,_) => error("cannot translate function types")
    }

  def translate_builtin_and_uninterpreted(
    fn_name:String, translated_arg_list:List[String]):String = {
    assert(builtins.contains(fn_name) || uninterpreted.contains(fn_name))
    (fn_name, translated_arg_list) match {
      case ("<", List(a, b)) => "(<= " + a + " (- " + b + " 1))"
      case (">", List(a, b)) => "(>= (- " + a + " 1) " + b + ")"
      case (_, _) =>
        val args = translated_arg_list.mkString(" ")
        fn_name match {
          case "unary-" => "(- " + args + ")"
          case "%" => "(mod " + args + ")"
          case "!=" => "(not (= " + args + "))"
          case "==" => "(= " + args + ")"
          case _ => "(" + fn_name + " " + args + ")"
        }
    }
  }

  // val declare_var : string -> 'a Expr.ty -> unit
  def declare_var[A](name:String, ty:Ty[A]) {
    val translated_ty = translate_ty(ty)
    Smt.write("(declare-const " + name + " " + translated_ty + ")")
  }

  // val var_name_map : (string, int) Hashtbl.t
  var var_name_map = Map[String,Int]()

  // val declare_new_var : 'a Expr.ty -> string
  def declare_new_var[A](ty:Ty[A]):String = {
    val var_name = real_ty(ty) match {
      case TConst(name) => name.substring(0, 1)
      case TApp(name, _) => name.substring(0, 1)
      case TVar(_) => "v"
      case TArrow(_,_) => error("cannot declare variables with function types")
    }
    val var_number =
      try {
        var_name_map(var_name)
      } catch {
        case _:Throwable => 0
      }
    
    var_name_map = var_name_map + (var_name -> (var_number + 1))
    val var_name2 = "_" + var_name + var_number
    declare_var(var_name2, ty)
    var_name2
  }


  // not sure if we need this
  /*
  def assert_true(if_clause, x) = {
    if_clause match {
      case None => Smt.write("(assert " + x + ")")
      case Some f => Smt.write("(assert (=> " + f + " " + x + "))")
    }
  }
  */

  // val assert_true : string -> unit
  def assert_true(translated_expr:String) {
    Smt.write("(assert " + translated_expr + ")")
  }

  def assert_false(translated_expr:String) {
    Smt.write("(assert (not " + translated_expr + "))")
  }

  def assert_eq(translated_expr1:String, translated_expr2:String) {
    assert_true("(= " + translated_expr1 + " " + translated_expr2 + ")")
  }

  def check_contract(if_clause:Option[String], fn_env:FnEnv.env,
    local_env:Map[String,String], contract_expr:t_expr) {
    Smt.push_pop { () =>
      if_clause match {
        case Some(translated_cond_expr) =>
          assert_true(translated_cond_expr)
        case None =>
      }
      assert_false(check_value(Formula, if_clause, fn_env, local_env, contract_expr))
      Smt.check_sat() match {
        case Unsat => /* OK */
        case Sat => error("SMT solver returned sat.")
        case Unknown => error("SMT solver returned unknown.")
        case Error(message) => error("SMT solver returned " + message + ".")
      }
    }
  }

  def check_function_subtype(if_clause:Option[String],
    fn_env:FnEnv.env, local_env:Map[String,String], fn_expr:t_expr, expected_fn_ty:t_ty) {
    val (closure_local_env:Map[String, String], fn_ty:t_ty) = check_function(if_clause, fn_env, local_env, fn_expr)
    val (param_r_ty_list:List[t_refined_ty], return_r_ty) = fn_ty match {
      case TArrow(param_r_ty_list, return_r_ty) => (param_r_ty_list, return_r_ty)
      case _ => throw new Exception("assert")
    }
    val (expected_param_r_ty_list:List[t_refined_ty], expected_return_r_ty) =
      expected_fn_ty match {
        case TArrow(expected_param_r_ty_list, expected_return_r_ty) =>
          (expected_param_r_ty_list, expected_return_r_ty)
        case _ => throw new Exception("assert")
      }
    Smt.push_pop {() =>
      val (new_closure_local_env, new_local_env) = param_r_ty_list.zip(expected_param_r_ty_list).foldLeft(
        (closure_local_env, local_env) 
      ){ case ((closure_local_env, local_env), (param_r_ty, expected_param_r_ty)) =>
          val (var_name, new_local_env) = expected_param_r_ty match {
            case Plain(ty) => (declare_new_var(ty), local_env)
            case Named(name, ty) =>
              val var_name = declare_new_var(ty)
              (var_name, LocalEnv.extend(name, var_name, local_env))
            case Refined(name, ty, expr:t_expr) =>
              val var_name = declare_new_var(ty)
              val new_local_env = LocalEnv.extend(name, var_name, local_env)
              assert_true(check_value(Formula, if_clause, fn_env, new_local_env, expr))
              (var_name, new_local_env)
          }
          val new_closure_local_env = param_r_ty match {
            case Plain(_) => closure_local_env
            case Named(name, _) => LocalEnv.extend(name, var_name, closure_local_env)
            case Refined(name, _, expr:t_expr) =>
              val new_closure_local_env = LocalEnv.extend(name, var_name, closure_local_env)
              check_contract(if_clause, fn_env, new_closure_local_env, expr)
              new_closure_local_env
          }
          (new_closure_local_env, new_local_env)
      }
      val return_var_name = declare_new_var(plain_ty(expected_return_r_ty))
      return_r_ty match {
        case Plain(_) | Named(_,_) =>
        case Refined(name, _, expr) =>
          val closure_return_ty_local_env =
            LocalEnv.extend(name, return_var_name, new_closure_local_env)
          assert_true(check_value(Formula, if_clause, fn_env, closure_return_ty_local_env, expr))
      }
      expected_return_r_ty match {
        case Plain(_) | Named(_,_) =>
        case Refined(name, _, expr) =>
          val return_ty_local_env = LocalEnv.extend(name, return_var_name, new_local_env)
          check_contract(if_clause, fn_env, return_ty_local_env, expr)
      }
    }
  }

  def check_function_call(if_clause:Option[String],
    fn_env:FnEnv.env, local_env:Map[String,String], fn_expr:t_expr, arg_expr_list:List[t_expr]
    ):(refined_ty[t_expr], List[String], Map[String,String]) = {
    val (closure_local_env, fn_ty) = check_function(if_clause, fn_env, local_env, fn_expr)
    val (param_r_ty_list, return_r_ty) = fn_ty match {
      case TArrow(param_r_ty_list, return_r_ty) => (param_r_ty_list, return_r_ty)
      case _ => throw new Exception("assert")
    }
    val (rev_translated_arg_expr_list, new_closure_local_env) =
      param_r_ty_list.zip(arg_expr_list).foldLeft((List[String](), closure_local_env)) {
      case ((rev_translated_arg_expr_list, closure_local_env), (param_r_ty, arg_expr)) =>
        if (is_function_ty(plain_ty(param_r_ty)))
          throw new Exception("not implemented - argument is a function")

        val (new_closure_local_env, translated_arg_expr) = param_r_ty match {
          case Plain(_) => (closure_local_env, check_value(Formula, if_clause, fn_env, local_env, arg_expr))
          case Named(name, _) =>
            val translated_arg_expr = check_value(Term, if_clause, fn_env, local_env, arg_expr)
            (LocalEnv.extend(name, translated_arg_expr, closure_local_env), translated_arg_expr)
          case Refined(name, _, expr) =>
            val translated_arg_expr = check_value(Term, if_clause, fn_env, local_env, arg_expr)
            val new_closure_local_env = LocalEnv.extend(name, translated_arg_expr, closure_local_env)
            check_contract(if_clause, fn_env, new_closure_local_env, expr)
            (new_closure_local_env, translated_arg_expr)
        }
        (translated_arg_expr :: rev_translated_arg_expr_list, new_closure_local_env)
      
    }
    
    val translated_arg_expr_list = rev_translated_arg_expr_list.reverse
    (return_r_ty, translated_arg_expr_list, new_closure_local_env)
  }

  def check_value(expected_result:Result, if_clause:Option[String],
    fn_env:Map[String, (Map[String,String], t_ty)],
    local_env:Map[String, String], expr:t_expr):String = {
    assert(!is_function_ty(expr.ty))

    expr.shape match {
      case EVar(name) => LocalEnv.lookup(name, local_env)
      case EBool(b) => translate_bool(b)
      case EInt(i) => translate_int(i)
      case ECall(fn_expr@t_expr(EVar(fn_name),_), arg_expr_list)
        if (builtins.contains(fn_name) || uninterpreted.contains(fn_name)) =>
          val (return_r_ty, translated_arg_expr_list, closure_local_env) =
            check_function_call(if_clause, fn_env, local_env, fn_expr, arg_expr_list)
          val translated_expr = translate_builtin_and_uninterpreted(fn_name, translated_arg_expr_list)
          expected_result match {
            case Formula => translated_expr
            case Term =>
              val var_name = declare_new_var(expr.ty)
              assert_eq(var_name, translated_expr)
              var_name
          }
      case ECall(fn_expr, arg_expr_list) =>
        val (return_r_ty, translated_arg_expr_list, closure_local_env) =
          check_function_call(if_clause, fn_env, local_env, fn_expr, arg_expr_list)
        return_r_ty match {
          case Plain(_) | Named(_,_) => declare_new_var(expr.ty)
          case Refined(name, _, contract_expr) =>
            val var_name = declare_new_var(expr.ty)
            val return_ty_local_env = LocalEnv.extend(name, var_name, closure_local_env)
            val translated_expr =
              check_value(Formula, if_clause, fn_env, return_ty_local_env, contract_expr)
            assert_true(translated_expr)
            var_name
        }
      case EFun(_, _, _) => throw new Exception("assert")
      case ELet(var_name, value_expr, body_expr) if (!is_function_ty(value_expr.ty)) =>
        val translated_value_expr = check_value(Formula, if_clause, fn_env, local_env, value_expr)
        declare_var(var_name, value_expr.ty)
        assert_eq(var_name, translated_value_expr)
        check_value(expected_result, if_clause, fn_env, local_env, body_expr)
      case ELet(fn_name, fn_expr, body_expr) /* when is_function_ty fn_expr.ty */ =>
        val local_env_and_ty = check_function(if_clause, fn_env, local_env, fn_expr)
        val new_fn_env = FnEnv.extend(fn_name, local_env_and_ty, fn_env)
        check_value(expected_result, if_clause, new_fn_env, local_env, body_expr)
      case EIf(cond_expr, then_expr, else_expr) =>
        val translated_cond_expr = check_value(Term, if_clause, fn_env, local_env, cond_expr)
        val (then_if_clause, else_if_clause) = if_clause match {
          case None => (Some(translated_cond_expr), Some("(not " + translated_cond_expr + ")"))
          case Some(translated_old_cond_expr) =>
            (
              Some("(and " + translated_old_cond_expr + " " + translated_cond_expr + ")"),
              Some("(and " + translated_old_cond_expr + " (not " + translated_cond_expr + "))")
            )
        }
        val translated_then_expr = check_value(Formula, then_if_clause, fn_env, local_env, then_expr)
        val translated_else_expr = check_value(Formula, else_if_clause, fn_env, local_env, else_expr)
        val translated_if_expr =
          "(ite " + translated_cond_expr +
            " " + translated_then_expr + " " + translated_else_expr + ")"
        expected_result match {
          case Formula => translated_if_expr
          case Term =>
            val var_name = declare_new_var(expr.ty)
            assert_eq(var_name, translated_if_expr)
            var_name
        }
      case ECast(expr, ty, Some(contract_expr)) =>
        val translated_expr = check_value(expected_result, if_clause, fn_env, local_env, expr)
        check_contract(if_clause, fn_env, local_env, contract_expr)
        translated_expr
      case ECast(expr, ty, None) => check_value(expected_result, if_clause, fn_env, local_env, expr)
    }
  }

  def check_function(if_clause:Option[String], fn_env:FnEnv.env,
    local_env:Map[String,String], expr:t_expr): (Map[String, String], t_ty) = {
    assert(is_function_ty(expr.ty))
    expr.shape match {
      case EVar(name) =>
        //assert (not ((StringSet.mem name builtins) || (StringSet.mem name uninterpreted)))
        FnEnv.lookup(name, fn_env)
      case EBool(_) | EInt(_) => throw new Exception("assert")
      case ECall(fn_expr, arg_expr_list) =>
        val (return_r_ty, translated_arg_expr_list, closure_local_env) =
          check_function_call(if_clause, fn_env, local_env, fn_expr, arg_expr_list)
        val return_ty1 = return_r_ty match {
          case Plain(return_ty) => return_ty
          case Named(_, return_ty) => return_ty
          case Refined(_,_,_) => error("cannot use refined type on an output function")
        }
        assert(is_function_ty(return_ty1))
        (closure_local_env, return_ty1)
      case EFun(param_list, maybe_return_r_ty, body_expr) if(is_function_ty(body_expr.ty)) =>
        error("not implemented - check_function - function returning a function")
      case EFun(param_list, maybe_return_r_ty, body_expr) =>
        Smt.push_pop { () =>
          val param_r_ty_list = param_list.map {
            case t_param(name, ty, None) =>
              declare_var(name, ty)
              Named(name, ty)
            case t_param(name, ty, Some(contract_expr)) =>
              declare_var(name, ty)
              assert_true(check_value(Formula, if_clause, fn_env, local_env, contract_expr))
              Refined(name, ty, contract_expr)
          }
          val return_r_ty = maybe_return_r_ty match {
            case Some(Refined(name, ty, expr)) =>
              val translated_body = check_value(Term, if_clause, fn_env, local_env, body_expr)
              val return_ty_local_env = LocalEnv.extend(name, translated_body, local_env)
              check_contract(if_clause, fn_env, return_ty_local_env, expr)
              Refined(name, ty, expr)
            case _ =>
              check_value(Formula, if_clause, fn_env, local_env, body_expr)
              Plain(body_expr.ty)
          }
          (LocalEnv.empty, TArrow(param_r_ty_list, return_r_ty))
        }
      case ELet(var_name, value_expr, body_expr) if (!is_function_ty(value_expr.ty)) =>
        val translated_value_expr = check_value(Formula, if_clause, fn_env, local_env, value_expr)
        declare_var(var_name, value_expr.ty)
        assert_eq(var_name, translated_value_expr)
        check_function(if_clause, fn_env, local_env, body_expr)
      case ELet(fn_name, fn_expr, body_expr) /* when is_function_ty fn_expr.ty */ =>
        val local_env_and_ty = check_function(if_clause, fn_env, local_env, fn_expr)
        val new_fn_env = FnEnv.extend(fn_name, local_env_and_ty, fn_env)
        check_function(if_clause, new_fn_env, local_env, body_expr)
      case EIf(_,_,_) => error("cannot use an if statement to select a function")
      case ECast(expr, ty, Some(contract_expr)) =>
        check_function_subtype(if_clause, fn_env, local_env, expr, ty)
        check_contract(if_clause, fn_env, local_env, contract_expr)
        (LocalEnv.empty, ty)
      case ECast(expr, ty, None) =>
        check_function_subtype(if_clause, fn_env, local_env, expr, ty)
        (LocalEnv.empty, ty)
    }
  }

  val global_fn_env =
    Infer.Env.fold(Core.env, FnEnv.empty){
      case (fn_name, fn_ty, fn_env) =>
        if (!is_function_ty(fn_ty))
          fn_env
        else
          FnEnv.extend(fn_name, (LocalEnv.empty, real_ty(fn_ty)), fn_env)
    }
  
  def declare_uninterpreted_function(fn_name:String, fn_ty:Ty[t_expr]) {
    /* Declares an uninterpreted symbol, for example
         length : forall[t] (a : array[t]) -> (l : int if l >= 0)
       is translated into
         (declare-fun length (Other) Int)
         (assert (forall ((a Other)) (>= (length a) 0)))
    */
    real_ty(fn_ty) match {
      case TArrow(param_r_ty_list, return_r_ty) =>
        val translated_param_list =
          param_r_ty_list.map {
            case Plain (_) => error("all parameters of uninterpreted functions must be named")
            case Named(name, ty) =>
              if (is_function_ty(ty))
                error("parameters of uninterpreted functions cannot be functions")
              else
                (name, translate_ty(ty))
            case Refined(name, ty, _) =>
              if (is_function_ty(ty))
                error("parameters of uninterpreted functions cannot be functions")
              else
                (name, translate_ty(ty))
          }
        if (is_function_ty(plain_ty(return_r_ty)))
          error("uninterpreted functions cannot return functions")
        val translated_return_ty = translate_ty(plain_ty(return_r_ty))
        Smt.write("(declare-fun " + fn_name +
          " (" + translated_param_list.map{_._2}.mkString(" ") + ") " +
          translated_return_ty + ")")
        return_r_ty match {
          case Plain(_) | Named(_,_) =>
          case Refined(name, return_ty, expr) =>
            val translated_param_list_str =
              translated_param_list.map {
                case(param_name, translated_param_ty) =>
                  "(" + param_name + " " + translated_param_ty + ")"
              }.mkString(" ")
            val param_name_list_str = translated_param_list.map{_._1}.mkString(" ")
            val result_str = "(" + fn_name + " " + param_name_list_str + ")"
            val local_env = LocalEnv.extend(name, result_str, LocalEnv.empty)
            val translated_expr = check_value(Formula, None, global_fn_env, local_env, expr)
            Smt.write("(assert (forall (" + translated_param_list_str + ") " +
                translated_expr + "))")
        }
      case _ => error("uninterpreted symbol " + fn_name + " must be a function")
    }
  }

  val already_started = Ref(false)

  def start() {
    if (!(already_started.a)) {
      already_started.a = true
      Smt.start()
      Smt.write("(declare-sort Other)")
      uninterpreted.foreach {
        fn_name =>
          declare_uninterpreted_function(fn_name, Infer.Env.lookup(fn_name, Core.env))
      }
      primitives.foreach {
        name =>
          val ty = Infer.Env.lookup(name, Core.env)
          if (!is_function_ty(ty)) declare_var(name, ty)
      }
      Smt.write("; End of global declarations.\n")
    }
  }

  def check_expr(expr:t_expr) {
    start()
    Smt.push_pop {
      () =>
        if (is_function_ty(expr.ty))
          check_function(None, global_fn_env, LocalEnv.empty, expr)
        else
          check_value(Formula, None, global_fn_env, LocalEnv.empty, expr)
    }
  }
  
}