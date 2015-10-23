# アルゴリズムWの訳者による解説

ここにあるソースはオリジナルのOCamlのコードをScalaに移植したものです。
アルゴリズムWは Damas-Hindley-Milner 型システムの型のオリジナルの推論アルゴリズムです。

`forall[a] -> a` のような多相型をサポートし、let一般化を行い、一般的な型を推論します。

アルゴリズムWは形式的には明示的な代入を使用して記述されています。
しかしここでは、更新可能な変数と、レベルを使った型の管理を行う事で、線形時間オーダーの計算量を達成しています。

## 実行方法

    sbt ~run

で実行できます。テストはただのメイン関数内にあります。

## ソースの概要

- `expr.scala` で構文木や型の定義やプリティプリントがあります。
- `parser.scala`でパーサコンビネータを使った簡単なパーサが実装されています。
- `infer.scala`でメイン処理の型推論が実装されています。
- `core.scala`ではテスト用の環境が含まれていて、
- `test_parser.scala`がパーサーのテストを、`test_infer.scala`が型推論のテストを行います。
- `test.scala`にはメインのクラスがありテスト全体を扱います。


型推論のメインの処理は式を`infer`で推論して、
２つの型を`unify`で同じ物であるとする単一化を行います。


- `unify`をする際に、再帰的に現れる型があると固まってしまうので出現チェックが必要になります。
    - `occurs_check_adjust_levels`で型の再入がないかを調べます。
- `infer`内で、多相的な関数を扱うには、
    - letバインドがあった場合に`generalize`で一般化し、
    - 変数の参照があった場合に`instantiate`で具体化します。
    - `infer`関数内の関数呼び出しは複雑なので補助関数の、`match_fun_ty`を使って引数と戻り値の処理をしています。

それでは、メインの処理のinfer.scalaをざっと見てみましょう。

    package dhm

    object Infer {

      import Expr._

next\_id, reset\_id は新しいIDを作ったり、IDのリセットを行います。

      var current_id = 0

      def next_id():Int = {
        val id = current_id
        current_id = id + 1
        id
      }

      def reset_id() {
        current_id = 0
      }

これは次の型変数を作成するときに使われます。

new\_var は新しい未束縛の型変数、new\_gen_varはGenericな新しい型変数をつくります。errorはエラー関数です。

      def new_var(tvar_level:level):Ty = TVar(Unbound(next_id(), tvar_level))

      def new_gen_var():Ty = TVar(Generic(next_id()))

      def error(msg:String):Nothing = throw new Exception(msg)

occurs\_check\_adjust\_levelsは再帰的な型の出現チェックを行うことで、無限ループを防ぎます。

      def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {
        def f(ty:Ty) {
          ty match {
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(_)) => assert(false)
            case other_tvar @ TVar(Unbound(other_id, other_level)) =>
              if (other_id == tvar_id) error("recursive types")          
              if (other_level > tvar_level)
                other_tvar.a = Unbound(other_id, tvar_level)
            case TApp(ty, ty_arg_list) =>
              f(ty)
              ty_arg_list.foreach(f)
            case TArrow(param_ty_list, return_ty) =>
              param_ty_list.foreach(f)
              f(return_ty)
            case TConst(_) =>
          }
        }
        f(ty)
      }

unifyは型２つを受け取って、型同士が同じになるように調整します。単一化と言います。

      def unify(ty1:Ty, ty2:Ty) {
        if (ty1 == ty2) return
        (ty1, ty2) match {
          case (TConst(name1), TConst(name2)) if(name1 == name2) =>
          case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
              ty_arg_list1.zip(ty_arg_list2).foreach{
                case (a,b) => unify(a, b)
              }
          case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
              param_ty_list1.zip(param_ty_list2).foreach{
                case(a,b) => unify(a, b)
              }
              unify(return_ty1, return_ty2)
          case (TVar(Link(ty1)), ty2) => unify(ty1, ty2)
          case (ty1, TVar(Link(ty2))) => unify(ty1, ty2)
          case (TVar(Unbound(id1, _)), TVar(Unbound(id2, _))) if id1 == id2 =>
              assert(false) // There is only a single instance of a particular type variable.
          case (tvar @ TVar(Unbound(id, tvar_level)), ty) =>
              occurs_check_adjust_levels(id, tvar_level, ty)
              tvar.a = Link(ty)
          case (ty, tvar @ TVar(Unbound(id, tvar_level))) =>
              occurs_check_adjust_levels(id, tvar_level, ty)
              tvar.a = Link(ty)
          case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
        }
      }

generalize一般化を行います。
let式で変数が定義されたときに呼び出され、型変数が未束縛で多相的に出来る場合に、一般化型変数に置き換えます。

      def generalize(level:level, ty:Ty):Ty = {
        ty match {
          case TVar(Unbound(id, other_level)) if other_level > level =>
            TVar(Generic(id))
          case TApp(ty, ty_arg_list) =>
            TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
          case TArrow(param_ty_list, return_ty) =>
            TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
          case TVar(Link(ty)) => generalize(level, ty)
          case TVar(Generic(_)) | TVar(Unbound(_, _)) | TConst(_) => ty
        }
      }

intantiateは一般化変数の具体化を行います。
変数が現れた場合に呼び出され、一般化型変数に新しい型変数を割り当てます。

      def instantiate(level:level, ty:Ty):Ty = {
        var id_var_map = Map[id,Ty]()
        def f (ty:Ty):Ty = {
          ty match {
            case TConst(_) => ty
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(id)) =>
              id_var_map.get(id) match {
                case Some(a) => a
                case None =>
                  val var1 = new_var(level)
                  id_var_map = id_var_map + (id -> var1)
                  var1
              }
            case TVar(Unbound(_,_)) => ty
            case TApp(ty, ty_arg_list) =>
              TApp(f(ty), ty_arg_list.map(f))
            case TArrow(param_ty_list, return_ty) =>
              TArrow(param_ty_list.map(f), f(return_ty))
          }
        }
        f(ty)
      }

inferは式を受け取り型を推論する型推論のエントリポイントです。

      def infer(env:Map[String,Ty], level:level, expr:Expr):Ty = {
        expr match {
          case Var(name) =>
            try {
              instantiate(level, env(name))
            } catch {
              case _:Throwable => error("variable " + name + " not found")
            }
          case Fun(param_list, body_expr) =>
            val param_ty_list = param_list.map{ _ => new_var(level)}
            val fn_env =
              param_list.zip(param_ty_list).foldLeft(env) {
                case(env, (param_name, param_ty)) => env + (param_name -> param_ty)
              }
            val return_ty = infer(fn_env, level, body_expr)
            TArrow(param_ty_list, return_ty)
          case Let(var_name, value_expr, body_expr) =>
            val var_ty = infer(env, level + 1, value_expr)
            val generalized_ty = generalize(level, var_ty)
            infer (env + (var_name -> generalized_ty), level, body_expr)
          case Call(fn_expr, arg_list) =>

match\_fun\_tyは関数の型のマッチングを行うinfer関数の補助関数です。

            def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
              ty match {
                case TArrow(param_ty_list, return_ty) =>
                  if (param_ty_list.length != num_params)
                    error("unexpected number of arguments")
                  (param_ty_list, return_ty)
                case TVar(Link(ty)) => match_fun_ty(num_params, ty)
                case tvar@TVar(Unbound(id, tvar_level)) =>
                  val param_ty_list = List.fill(num_params){new_var(tvar_level)}
                  val return_ty = new_var(tvar_level)
                  tvar.a = Link(TArrow(param_ty_list, return_ty))
                  (param_ty_list, return_ty)
                case _ => error("expected a function")
              }
            }

            val (param_ty_list, return_ty) =
              match_fun_ty(arg_list.length, infer(env, level, fn_expr))
            param_ty_list.zip(arg_list).foreach{
              case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
            } 
            return_ty
        }
      }
    }


まとめると以下のような関数がありました。

- next_id idを生成
- reset_id idのカウンタをリセット
- new_var 型変数を作成
- new\_gen\_var 一般化型変数を作成

- infer 型推論
    - generalize 一般化
    - instantiate 具体化
    - match\_fun\_ty 関数の補助関数
- unify 単一化
    - occurs\_check\_adjust\_levels 出現チェック


## 最適化について

このアルゴリズムでは２つの最適化技法を使っています。

１つ目は、書き換え可能な変数を使って、代入の操作を高速化します。
型の代入の方程式を作って後から代入するのではなく、式のトラバース中にその場でLinkを使って代入操作をすることで高速化します。これはPrologからの伝統の単一化のやり方です。

２つ目は、ネストレベルを使った多相的な型の管理を行います。

型変数にネストレベルもたせ、また型推論時に式のネストレベルを管理する事で型の生存チェックを行います。
レベルを使った型推論のプログラムでは以下のような処理を行います。

- new_var関数で新しい型変数を作る時に受け取ったレベルを保存します。
- infer関数で型推論時にletの最初の式のときは現状レベルを１つ深くします。
- また、関数呼び出しの引数は未束縛な型変数に含まれているレベルで型変数を作ります。
- unify関数で単一化時に型変数中のレベルtvar_levelを取り出して、出現チェックを呼び出します。
- 出現チェック時には他の型に型変数が現れたらレベルをtvar\_levelにあわせて浅くします。
- 一般化時に型変数のレベルが現在のレベルより深い時だけ一般化します。
- 具現化時に一般化変数は型変数を作り現状のlevelを保存して置き換えます。

ネストが深いところで具体化された型変数が、浅いところを通って、次の深い場所で参照された場合に型変数が一般化対象にならないようにするため、浅い場所に行った場合には浅いほうにあわせます。
一般化時にはネストが深いところで定義された変数だけを一般化します。
具体化時にはとくにレベルを気にする事なく一般化型変数を具体化します。

## レベルに注目してソースを読む

それでは、もう一度レベルに注目してソースを読んでみましょう。

level変数で現在のスコープレベルを表し引き回しています。型変数のレベルを参照だけするときはother\_level、コピーする可能性がある時はtvar\_levelという名前を使用しています。
level変数は文脈ですので特にコメントしていません。文脈が変わるところだけ注意してみてください。


new\_var は新しい未束縛の型変数をつくりますがそのときにtvar_levelを保持します。

      def new_var(tvar_level:level):Ty = TVar(Unbound(next_id(), tvar_level))

      def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {
        def f(ty:Ty) {
          ty match {
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(_)) => assert(false)
            case other_tvar @ TVar(Unbound(other_id, other_level)) =>
              if (other_id == tvar_id) error("recursive types")          

出現チェック関数では、tvar_levelを受け取って他の型のレベルと比較します。
型のレベルが渡されたレベルより深ければ、手前のスコープがもう終わっていたので、渡された深さに浅くします。

              if (other_level > tvar_level)
                other_tvar.a = Unbound(other_id, tvar_level)
            case TApp(ty, ty_arg_list) =>
              f(ty)
              ty_arg_list.foreach(f)
            case TArrow(param_ty_list, return_ty) =>
              param_ty_list.foreach(f)
              f(return_ty)
            case TConst(_) =>
          }
        }
        f(ty)
      }

      def unify(ty1:Ty, ty2:Ty) {
        if (ty1 == ty2) return
        (ty1, ty2) match {
          case (TConst(name1), TConst(name2)) if(name1 == name2) =>
          case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
              ty_arg_list1.zip(ty_arg_list2).foreach{
                case (a,b) => unify(a, b)
              }
          case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
              param_ty_list1.zip(param_ty_list2).foreach{
                case(a,b) => unify(a, b)
              }
              unify(return_ty1, return_ty2)
          case (TVar(Link(ty1)), ty2) => unify(ty1, ty2)
          case (ty1, TVar(Link(ty2))) => unify(ty1, ty2)
          case (TVar(Unbound(id1, _)), TVar(Unbound(id2, _))) if id1 == id2 =>
              assert(false) // There is only a single instance of a particular type variable.

単一化関数では型変数のレベルを取り出して、occurs\_check\_adjust\_levelsに渡します。

          case (tvar @ TVar(Unbound(id, tvar_level)), ty) =>
              occurs_check_adjust_levels(id, tvar_level, ty)
              tvar.a = Link(ty)

          case (ty, tvar @ TVar(Unbound(id, tvar_level))) =>
              occurs_check_adjust_levels(id, tvar_level, ty)
              tvar.a = Link(ty)

          case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
        }
      }

      def generalize(level:level, ty:Ty):Ty = {
        ty match {

型変数があったら、レベルが深い時だけ、一般化します。

          case TVar(Unbound(id, other_level)) if other_level > level =>
            TVar(Generic(id))

          case TApp(ty, ty_arg_list) =>
            TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
          case TArrow(param_ty_list, return_ty) =>
            TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
          case TVar(Link(ty)) => generalize(level, ty)
          case TVar(Generic(_)) | TVar(Unbound(_, _)) | TConst(_) => ty
        }
      }

      def instantiate(level:level, ty:Ty):Ty = {
        var id_var_map = Map[id,Ty]()
        def f (ty:Ty):Ty = {
          ty match {
            case TConst(_) => ty
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(id)) =>
              id_var_map.get(id) match {
                case Some(a) => a
                case None =>

具体化するときは型変数を作るときに現状のlevelを保存します。

                  val var1 = new_var(level)
                  id_var_map = id_var_map + (id -> var1)
                  var1
              }
            case TVar(Unbound(_,_)) => ty
            case TApp(ty, ty_arg_list) =>
              TApp(f(ty), ty_arg_list.map(f))
            case TArrow(param_ty_list, return_ty) =>
              TArrow(param_ty_list.map(f), f(return_ty))
          }
        }
        f(ty)
      }

      def infer(env:Map[String,Ty], level:level, expr:Expr):Ty = {
        expr match {
          case Var(name) =>
            try {
              instantiate(level, env(name))
            } catch {
              case _:Throwable => error("variable " + name + " not found")
            }
          case Fun(param_list, body_expr) =>

関数の型推論時は関数の型引数用にレベル付きで型変数を作ります。

            val param_ty_list = param_list.map{ _ => new_var(level)}
            val fn_env =
              param_list.zip(param_ty_list).foldLeft(env) {
                case(env, (param_name, param_ty)) => env + (param_name -> param_ty)
              }

            val return_ty = infer(fn_env, level, body_expr)
            TArrow(param_ty_list, return_ty)
          case Let(var_name, value_expr, body_expr) =>

Letのときは型推論呼び出す時にレベルを上げます。引数で文脈として引き回すので終わったら元に戻ります。

            val var_ty = infer(env, level + 1, value_expr)
            val generalized_ty = generalize(level, var_ty)
            infer (env + (var_name -> generalized_ty), level, body_expr)
          case Call(fn_expr, arg_list) =>
            def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
              ty match {
                case TArrow(param_ty_list, return_ty) =>
                  if (param_ty_list.length != num_params)
                    error("unexpected number of arguments")
                  (param_ty_list, return_ty)
                case TVar(Link(ty)) => match_fun_ty(num_params, ty)

型変数の時は、そのレベルで、引数とリターン値用の新しい型変数をつくります。

                case tvar@TVar(Unbound(id, tvar_level)) =>
                  val param_ty_list = List.fill(num_params){new_var(tvar_level)}
                  val return_ty = new_var(tvar_level)
                  tvar.a = Link(TArrow(param_ty_list, return_ty))
                  (param_ty_list, return_ty)
                case _ => error("expected a function")
              }
            }

            val (param_ty_list, return_ty) =
              match_fun_ty(arg_list.length, infer(env, level, fn_expr))
            param_ty_list.zip(arg_list).foreach{
              case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
            } 
            return_ty
        }
      }
    }

## 発展について

Olegの解説ではさらに最適なレベルを使った一般化をするアルゴリズムがある訳ですが、そこまではこの例では扱っていません。

更なる最適化では、

1. 一般化変数の型をなくして、Unboundのレベルに一般化レベルを作ります。
2. 型の複数のリンクを辿らないように、出来るだけリンクが無くなるようにします。
3. 全ての型にレベルを持たせ、インスタンス化時に単相型のトラバースを避け、一般化時にもレベルを見て無駄なトラバースを避けます。
4. 最後に単一化時の型チェックのトラバースを避けて必要になるまで遅延させます。
    この場合、再入した型が現れる可能性があるのでmark&sweepのマークビットのようなマークレベルを使って再入を防ぎ、サイクルを発見します。

特に重要な最適化は２つで、インスタンス化と一般化のトラバースの削減と、出現チェックを遅延させてトラバースを削減する事です。

## まとめ

この解説では、もう2回程ソースを見直しました。最初に概要を見て、次にレベルに注目して見ました。

この文書で使われているアルゴリズムはOlegの解説のプログラムを多少変えて、enter,leaveを呼ばずに、levelを引き回したり、型変数の定義を変えてシンプルにしたものです。
さらにScalaに移植することでMLの文法になれていない人でも読みやすくなったかと思います。
括弧がある分行数は増えましたが、Scalaの集合やMapのライブラリは分かりやすいのでEnvモジュールを消しました。
また、参照は書き換え可能な変数に書き換えました。
日本語に翻訳することで日本人に取って分かりやすくし、直訳的な文章では分かり辛いかもしれないのでもう一度、
その解説を書いているのがこの解説です。

多相的な型推論の基本的な高速なアルゴリズムの実装があり、解説があるので参考にしてもらえば幸いです。

## EBNF

    ident       ::= [_A-Za-z][_A-Za-z0-9]*
    integer     ::= [0-9]+

    expr        ::= app_expr
                  | "let" ident "=" expr "in" expr
                  | "fun" rep1(ident) "->" expr

    app_expr    ::= simple_expr rep("(" rep1sep(expr, ",") ")")

    simple_expr ::= ident
                  | "(" expr ")"

    ty_forall   ::= ty
                  | "forall" "[" rep1(ident) "]" ty

    ty          ::= app_ty "->" ty
                  | app_ty
                  | "(" repsep(ty, ",") ")" "->" ty

    app_ty      ::= simple_ty rep("[" rep1sep(ty, ",") "]")

    simple_ty   ::= ident
                  | "(" ty ")"
