object test extends App{
  type varname = String

  trait exp 
  case class Var(a:varname) extends exp
  case class Call(a:exp, b:exp) extends exp
  case class Fun(a:varname, b:exp) extends exp
  case class Let(a:varname, b:exp, c:exp) extends exp

  // 型から一般化された型を消して、generic_levelなら一般化されていることにする

  type level = Int
  val generic_level = 100000000           // as in OCaml typing/btype.ml
  val marked_level = -1                   // 再帰検査中を示すフラグ
  trait typ 
  case class TVar(var a:tv) extends typ
  case class TArrow(a:typ, b:typ, c:levels) extends typ
  trait tv
  case class Unbound(a:String, b:level) extends tv
  case class Link(a:typ) extends tv

  // 関数内のレベル保存用
  case class levels(var level_old : level, var level_new : level)

  var count = 0
  def reset_gensym () { count = 0 }

  // 型変数生成
  def newvar(level:level):typ = {
    def gensym() = {
      val i = count
      count += 1
      (97 + i % 26).toChar.toString + (if (i >= 26) ""+(i / 26) else "")
    }
    TVar(Unbound(gensym(), level))
  }
  // 関数生成
  def new_arrow(level:level,ty1:typ, ty2:typ):typ =
    TArrow(ty1,ty2,levels(level_new = level, level_old = level))

  // 型のリンクを外して回る。
  def repr(typ:typ): typ =
    typ match {
    case tvr @ TVar(Link(t)) => 
        val t1 = repr(t)
        tvr.a = Link(t1); t1
    case t => t
    }

  var occurs_check_queue = List[typ]()
  def reset_occurs_check_queue() { occurs_check_queue = List() }

  type env = Map[varname, typ]

  def error(msg:String):Nothing = throw new Exception(msg)

  def unify(t1:typ, t2:typ) {
    if (t1 == t2) return
    (repr(t1), repr(t2)) match {
    case (t1@TVar(Unbound(_,l1)), t2@TVar(Unbound(_,l2))) =>
        // bind the higher-level var
        if (l1 > l2) t1.a = Link(t2) else t2.a = Link(t1)
    case (tv@TVar(Unbound(_,l)), t) =>
        update_level(l, t)
        tv.a = Link(t)
    case (t, tv@TVar(Unbound(_,l))) => 
        update_level(l, t)
        tv.a = Link(t)
    case (TArrow(tyl1,tyl2,ll), TArrow(tyr1,tyr2,lr)) =>
        def unify_lev(l:level, ty1:typ, ty2:typ) {
          val ty = repr(ty1)
          update_level(l, ty)
          unify(ty, ty2)
        }
        // unify_levで再帰的に呼ばれた場合チェックが必要。
        if (ll.level_new == marked_level || lr.level_new == marked_level)
          error("cycle: occurs check")
        val min_level = Math.min(ll.level_new, lr.level_new)
        ll.level_new = marked_level; lr.level_new = marked_level;
        unify_lev(min_level, tyl1, tyr1)
        unify_lev(min_level, tyl2, tyr2)
        ll.level_new = min_level; lr.level_new = min_level
    case _ => assert(false)
    }

    // 受け取った型のレベルを変更する
    def update_level(level:level, typ:typ) {
      typ match {
      // 型変数の場合
      case tvr@TVar(Unbound(n, level1)) => 
          // 一般化変数は有り得ない
          assert(level1 != generic_level)
          // 変数のレベルが深い場合、浅いほうへ持ち上げる
          if (level < level1)
            tvr.a = Unbound(n,level)
      // 関数のレベルを再入せずに更新する
      case ty @ TArrow(_,_,ls) => 
          // 一般化変数は有り得ない
          assert(ls.level_new != generic_level)
          // レベルがマークレベルなら出現チェックエラー
          if (ls.level_new == marked_level) error("occurs check")

          // 変数のレベルが深い場合、浅いほうへ持ち上げる
          if (level < ls.level_new) {
            // level_newとlevel_oldが同じ場合は、遅延リストに入っていない
            if (ls.level_new == ls.level_old)
              // 遅延用リストに加える
              occurs_check_queue = ty :: occurs_check_queue
            // レベルを更新するとlevel_newとlevel_oldは変わる
            ls.level_new = level
          }
      case _ => assert(false)
      }
    }
  }

  def generalize(level:level, ty:typ) {
    force_occurs_check(level)
    loop(ty)

    def force_occurs_check(level:level) {
      occurs_check_queue =
        occurs_check_queue.foldLeft(List[typ]())(occurs_check)
      def occurs_check(acc:List[typ],typ:typ):List[typ] =
        typ match {
        case ty@TArrow(_, _, ls) if ls.level_old <= level =>
            ty::acc                         // update later
        case TArrow(_, _, ls) if ls.level_old == ls.level_new =>
            acc                             // already updated
        case TArrow(ty1, ty2, ls) =>
            val level = ls.level_new
            ls.level_new = marked_level
            val acc1 = loop(acc, level, ty1)
            val acc2 = loop(acc1, level, ty2)
            ls.level_new = level
            ls.level_old = level
            acc2
        case _ => error("assert")
        }
      def loop(acc:List[typ], level:level, ty:typ):List[typ] = 
        repr(ty) match {
        case tv@TVar(Unbound(name,l)) if l > level =>
            tv.a = Unbound(name,level); acc
        case TArrow(_,_,ls) if ls.level_new == marked_level =>
            error("occurs check")
        case TArrow(_,_,ls) =>
            if (ls.level_new > level) ls.level_new = level
            occurs_check(acc, ty)
        case _ => acc
        }
    }

    // 型からレベル取得
    def get_level(typ:typ):level = {
      typ match {
        case TVar(Unbound (_,l)) => l
        case TArrow(_,_,ls) => ls.level_new
        case _ => error("assert")
      }
    }
    def loop(ty:typ) {
      repr(ty) match {
      case tv@TVar(Unbound(name, l)) if l > level =>
        tv.a = Unbound(name, generic_level)
      case TArrow(t1,t2,ls) if ls.level_new > level =>
        val ty1 = repr(t1)
        val ty2 = repr(t2)
        loop(ty1); loop(ty2)
        val l = Math.max(get_level(ty1),get_level(ty2))
        ls.level_old = l; ls.level_new = l // set the exact level upper bound
      case _ =>
      }
    }
  }

  def instantiate(level:level, ty:typ): typ = {
    def loop(subst:Map[String,typ], ty:typ):(typ, Map[String,typ]) =
      ty match {
      case TVar(Unbound (name,l)) if l == generic_level => 
          subst.get(name) match {
          case Some(v) => (v, subst)
          case None =>
              val tv = newvar(level)
              (tv, subst + (name -> tv))
          }
      case TVar(Link(ty)) => loop(subst, ty)
      case TArrow(t1,t2,ls) if ls.level_new == generic_level =>
          val (ty1,subst1) = loop(subst, t1)
          val (ty2,subst2) = loop(subst1, t2)
          (new_arrow(level, ty1, ty2), subst2)
      case ty => (ty, subst)
      }
    loop(Map[String,typ](), ty)._1
  }

  def infer(level:level, env:env, exp:exp): typ = {
    exp match {
    case Var(x) => instantiate(level, env(x))
    case Fun(x, e) => 
        val ty_x = newvar(level)
        val ty_e = infer(level, env + (x -> ty_x), e)
        new_arrow(level, ty_x, ty_e)
    case Call(e1,e2) =>
        val ty_fun = infer(level, env, e1)
        val ty_arg = infer(level, env, e2)
        val ty_res = newvar(level)
        unify(ty_fun, new_arrow(level, ty_arg, ty_res))
        ty_res
    case Let(x,e,e2) => 
        val ty_e = infer(level + 1, env, e)
        generalize(level, ty_e)
        infer(level, env + (x -> ty_e), e2)
    }
  }
  // 再帰の型チェック
  def final_occurs_check(typ:typ){
    typ match {
      case TVar(Unbound(_,_)) =>
      case TVar(Link(ty))     => final_occurs_check(ty)
      case TArrow(_,_,ls) if ls.level_new == marked_level => error("occurs check")
      case TArrow(t1,t2,ls) =>
        val level = ls.level_new
        ls.level_new = marked_level
        final_occurs_check(t1)
        final_occurs_check(t2)
        ls.level_new = level
    }
  }
  val id = Fun("x",Var("x"))
  val c1 = Fun("x",Fun("y",Call(Var("x"),Var("y"))))

  def top_type_check(exp:exp):typ = {
    reset_gensym()
    reset_occurs_check_queue()
    val ty = infer(1, Map(), exp)
    final_occurs_check(ty)
    ty
  }

  def newlevels():levels = levels(1, 1)

  assert(
    TArrow(TVar(Unbound("a", 1)),
     TVar(Unbound("a", 1)), newlevels())
       ==
      top_type_check(id))

  assert(
    TArrow(TVar(Link(
      TArrow(TVar(Unbound("b", 1)),
             TVar(Unbound("c", 1)), newlevels()))),
     TArrow(TVar(Unbound("b", 1)),
            TVar(Unbound("c", 1)), newlevels()),
     newlevels())
     ==
       top_type_check(c1))

  assert(
    TArrow
     (TArrow(TVar(Unbound("d", 1)),
       TVar(Unbound("e", 1)), newlevels()),
     TArrow(TVar(Unbound("d", 1)),
      TVar(Unbound("e", 1)), newlevels()),
     newlevels())
     ==
     top_type_check(Let("x",c1,Var("x"))))

  assert(
    TArrow(TVar(Unbound("b", 1)),
     TVar(Unbound("b", 1)), newlevels())
     ==
     top_type_check(Let("y",Fun("z",Var("z")), Var("y"))))

  assert(
    TArrow(TVar(Unbound("a", 1)),
     TArrow(TVar(Unbound("c", 1)),
      TVar(Unbound("c", 1)), newlevels()),
     newlevels())
     ==
     top_type_check(Fun("x", Let("y",Fun("z",Var("z")), Var("y")))))

  assert(
    TArrow(TVar(Link(TVar(Unbound("c", 1)))),
     TVar(Link(TVar(Unbound("c", 1)))),
     newlevels())
     ==
     top_type_check(Fun("x", Let("y",Fun("z",Var("z")), 
                                        Call(Var("y"),Var("x"))))))

  try { 
    top_type_check(Fun("x",Call(Var("x"),Var("x"))))
    assert(false)
  } catch {
    case e:Exception => println(e.getMessage())
  }

  try { 
    top_type_check(Let("x",Var("x"),Var("x")))
    assert(false)
  } catch {
    case e:Throwable => "unbound var"
  }

  assert(
    TVar(
       Link
        (TArrow(TVar(Unbound("c", 1)),
          TVar(Unbound("c", 1)), newlevels())))
     ==
     top_type_check(Let("id",id, Call(Var("id"),Var("id")))))

  assert(
    TArrow(TVar(Unbound("i", 1)),
     TVar(Unbound("i", 1)), newlevels())
     ==
     top_type_check(Let("x",c1,
                        Let("y",
                              Let("z",Call(Var("x"),id), Var("z")),
                             Var("y")))));

  /*
  fun x -> fun y -> let x = x y in fun x -> y x;;
  - : (('a -> 'b) -> 'c) -> ('a -> 'b) -> 'a -> 'b = <fun>
  */
  assert(
    TArrow
     (TVar(
         Link
          (TArrow
            (TVar(
                Link
                 (TArrow(TVar(Unbound("d", 1)),
                   TVar(Unbound("e", 1)),
                   newlevels()))),
            TVar(Unbound("c", 1)), newlevels()))),
     TArrow
      (TVar
          (Link
           (TArrow(TVar(Unbound("d", 1)),
             TVar(Unbound("e", 1)), newlevels()))),
      TArrow(TVar(Unbound("d", 1)),
       TVar(Unbound("e", 1)), newlevels()),
      newlevels()),
     newlevels())
     ==
     top_type_check(Fun("x", Fun("y",Let("x",Call(Var("x"),Var("y")),
                                      Fun("x",Call(Var("y"),Var("x"))))))))


  // now sound generalization !
  assert(
    TArrow(TVar(Unbound("a", 1)),
     TVar(Unbound("a", 1)), newlevels())
     ==
     top_type_check(Fun("x", Let("y",Var("x"), Var("y")))))

  // now sound generalization !
  assert(
    TArrow(TVar(Unbound("a", 1)),
     TArrow(TVar(Unbound("c", 1)),
      TVar(Unbound("a", 1)), newlevels()),
     newlevels())
     ==
     top_type_check(Fun("x", Let("y",Fun("z",Var("x")), Var("y")))))

  // now sound generalization !
  assert(
    TArrow
     (TVar(
         Link
          (TArrow(TVar(Unbound("b", 1)),
            TVar(Unbound("c", 1)), newlevels()))),
     TArrow(TVar(Unbound("b", 1)),
      TVar(Unbound("c", 1)), newlevels()),
     newlevels())
     ==
     top_type_check(Fun("x", Let("y",Fun("z",Call(Var("x"),Var("z"))), Var("y")))))

  // now sound generalization !
  assert(
    TArrow
     (TVar(
         Link
          (TArrow(TVar(Unbound("b", 1)),
            TVar(
               Link
                (TArrow(TVar(Unbound("b", 1)),
                  TVar(Unbound("d", 1)),
                  newlevels()))),
            newlevels()))),
     TArrow(TVar(Unbound("b", 1)),
      TVar(Unbound("d", 1)), newlevels()),
     newlevels())
     ==
     top_type_check(Fun("x", Fun("y",Let("x",Call(Var("x"),Var("y")),
                                        Call(Var("x"),Var("y")))))))

  // now sound generalization !
  try { 
    top_type_check(Fun("x",Let("y",Var("x"), Call(Var("y"),Var("y")))))
    assert(false)
  } catch {
    case e:Exception => println(e.getMessage())
  }

  // now sound generalization !
  // fun x -> let y = let z = x (fun x -> x) in z in y;;
  // - : (('a -> 'a) -> 'b) -> 'b = <fun>
  assert(
    TArrow
     (TVar(
         Link
          (TArrow
            (TArrow(TVar(Unbound("b", 1)),
              TVar(Unbound("b", 1)), newlevels()),
            TVar(Unbound("c", 1)), newlevels()))),
     TVar(Unbound("c", 1)), newlevels())
     ==
     top_type_check(Fun("x",
                        Let("y",
                              Let("z",Call(Var("x"),id), Var("z")),
                              Var("y")))));
  println("\nAll Done\n")
}
