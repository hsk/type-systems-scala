# アルゴリズムWの訳者による解説

ここにあるソースはオリジナルのOCamlのコードをScalaに移植したものです。

## 実行方法

    sbt ~run

で実行できます。テストはただのメイン関数内にあります。


## ソースの概要

これは、OCamlの多相的な型推論をレベルを使って高速化したプログラムの簡単な例です。

メインの処理は、infer.scalaにあるので、infer.scalaをもう一度見てみましょう。

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

      def new_var(level:level):Ty = TVar(Unbound(next_id(), level))

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
          case (tvar @ TVar(Unbound(id, level)), ty) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)
          case (ty, tvar @ TVar(Unbound(id, level))) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)
          case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
        }
      }

generalizeは変数が定義されたときに呼び出され、値が多相的だった場合に、一般化された型パラメータに置き換えます。

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

intantiateは変数が現れた場合に呼び出され、一般化された型パラメータを逆に具体化します。

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

match\_fun\_tyは関数の型のマッチングを行うinfer関数の補助関数です。

      def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
        ty match {
          case TArrow(param_ty_list, return_ty) =>
            if (param_ty_list.length != num_params)
              error("unexpected number of arguments")
            (param_ty_list, return_ty)
          case TVar(Link(ty)) => match_fun_ty(num_params, ty)
          case tvar@TVar(Unbound(id, level)) =>
            val param_ty_list = List.fill(num_params){new_var(level)}
            val return_ty = new_var(level)
            tvar.a = Link(TArrow(param_ty_list, return_ty))
            (param_ty_list, return_ty)
          case _ => error("expected a function")
        }
      }

inferが式を受け取り型を推論する型推論のエントリポイントです。

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
            val (param_ty_list, return_ty) =
              match_fun_ty(arg_list.length, infer(env, level, fn_expr))
            param_ty_list.zip(arg_list).foreach{
              case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
            } 
            return_ty
        }
      }
    }


OCamlからの移植なので、プログラムは上からボトムアップ的に書かれています。トップダウン的に見たければ、最後の関数から読んで行くと良いでしょう。

このアルゴリズムは大ざっぱに言うと、式を`infer`で推論して、２つの型を`unify`で同じ物であるとする単一化を行います。
`unify`をする際に、再帰的に現れる型があると固まってしまうので出現チェックが必要になります。そこで`occurs_check_adjust_levels`で調べます。
多相的な関数を扱うには、letバインドがあった場合に`generalize`で一般化して、変数の参照があった場合に`instantiate`で具体化する事が必要です。
`infer`関数内の関数呼び出しは複雑になるので補助関数の、`match_fun_ty`を使って引数と戻り値の処理をしています。

## 最適化について

このアルゴリズムでは２つの最適化技法を使っています。

１つ目は、書き換え可能な変数を使うことで、代入の操作を高速化しています。
型推論は、型の方程式を作って分からない型へ分かっている型を代入する事で推論できるのですが、代入の式をたくさん作って後で計算するよりもその場で代入操作をしてしまったほうが速い訳です。破壊的な変更が必要になりますが、コンパイルは高速なほうがいいですよね。これは単相の型推論を高速化するアルゴリズムです。

２つ目は、レベルを使った多相的な型推論の最適化です。

多相的な型推論をするには、通常型スキームと呼ばれる、テンプレートの集合のような物を使います。
アルゴリズムWの多相的な一般化するアルゴリズムは基本的に遅く、Olegの解説によると

https://github.com/hsk/docs/blob/master/generalization/2.%E4%B8%80%E8%88%AC%E5%8C%96.md

レミーはCamlのコンパイルは非常に重くて20分かかったので、もっと速くしたかったそうです。

そこで多相的な一般化も書き換え可能な変数を使って高速したアイディアが生まれます。

とりあえず作ってみた例が3.メモリ管理ミスがある不完全な一般化です。

https://github.com/hsk/docs/blob/master/generalization/3.%E3%83%A1%E3%83%A2%E3%83%AA%E7%AE%A1%E7%90%86%E3%83%9F%E3%82%B9%E3%81%8C%E3%81%82%E3%82%8B%E4%B8%8D%E5%AE%8C%E5%85%A8%E3%81%AA%E4%B8%80%E8%88%AC%E5%8C%96.md

残念ながら、単純に多相的な物に適用しようとすると無理があったので、例えば以下のような推論をしてします。

    fun x y -> let x = x y in x y : (b -> c) -> b -> e


そこで、レベルの考え方を導入したのが、4.レベルによる効率的な一般化です。

https://github.com/hsk/docs/blob/master/generalization/4.%E3%83%AC%E3%83%99%E3%83%AB%E3%81%AB%E3%82%88%E3%82%8B%E5%8A%B9%E7%8E%87%E7%9A%84%E3%81%AA%E4%B8%80%E8%88%AC%E5%8C%96.md

多相的な関数の推論にスコープ的な要素を加えて完全な物としたので以下のように推論されます。

    fun x y -> let x = x y in x y : (b -> b -> d) -> b -> d

次のレベルについてで、詳しく考えてみましょう。

## レベルの例

以下の式をレベルを意識せずに推論する事を考えましょう。

    fun x y -> let z = x y in z y : (b -> b -> d) -> b -> d

すると以下のように考える事が出来ます。

    fun x y で、
    xの型は未束縛の型変数x0です。
    yの型に未束縛の型変数y0です。

    let z = x y内のx yを考えると
    x yの型をz0とすると、
    xの型はyの型を受け取りz0を返す y0 -> z0です。
    let z = x yを考えると
    zの型はz0を一般化して、z'です。

    z yを考えると、
    z’ は具体化して未束縛の型変数z'0であり、
    z'0はy0を受け取ってr0を返す、y0 -> r0です。

    関数全体の型は
    x0 -> y0 -> r0で、x0はy0 -> z0でもあります。

    これらの方程式をとくと、

    (y0 -> z0) -> y0 -> r0となります。

ここは、じっくり考えてみてください。z0が一般化されてしまっては困ることが分かるはずです。

ここで、スコープのレベルという概念を持って、スコープレベルが上がったときには、レベルの低い物は一般化しないようにします。型変数にレベルを持たせて、一般化する時はレベルが低い物は一般化しないようにします。

レベルを意識するアルゴリズムでは次のようにします。

    fun x y -> let z = x y in z y : (b -> b -> d) -> b -> d

    fun x yのレベル0で戻りの型をr0と考えると

    関数全体の型=x0 -> y0 -> r0

    xの型=x0
    yの型=y0

    let z = x y で x yの型=z0とする。このzのレベルはxのレベル0にあわせる

    xの型=y0 -> z0

    let z = x yの一般化を考えると、
    zの型はz0をレベル0で一般化しようとしても一般化されず、z0のままである。ここがポイントだ。
    レベルを意識しなければ一般化されるところである。

    z yの型を考えると

    z yの型=r0

    zの型は具体化しても一般化されてないのでyの型はy0なので

    zの型=z0
    yの型=y0
    zの型=y0 -> r0

    関数全体の型=x0 -> y0 -> r0 に xの型=x0とxの型=y0 -> z0 から

    関数全体の型=(y0 -> z0) -> y0 -> r0

    zの型=z0かつzの型=y0 -> r0から

    関数全体の型=(y0 -> y0 -> r0) -> y0 -> r0

一般化するときに、レベルが低い時は一般化しないことにすることがポイントです。

コレで万事をオッケーかというと抜けがあります。unifyするときに、参照している型変数のレベルが現在レベルより高い時があるとしたら、それは一度レベルの高いところで参照されていた物でしょう。だけど、もう、今のレベルから見られているので下げる必要があります。だから、出現チェック時にはレベルを下げます。

## レベルに注目してソースを読む

もう一度レベルに注目してソースを読んでみましょう。

new\_var は新しい未束縛の型変数をつくりますがそのときにlevelを保持します。

      def new_var(level:level):Ty = TVar(Unbound(next_id(), level))

occurs\_check\_adjust\_levelsは再帰的な型の出現チェックを行い、無限ループを防ぎます。

      def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {
        def f(ty:Ty) {
          ty match {
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(_)) => assert(false)
            case other_tvar @ TVar(Unbound(other_id, other_level)) =>
              if (other_id == tvar_id) error("recursive types")          

ここでは、tvar_levelを受け取って型のレベルと比較します。

              if (other_level > tvar_level)
                other_tvar.a = Unbound(other_id, tvar_level)

型のレベルが渡されたレベルより大きければ、手前のスコープがもう終わってたので、渡されたレベルに下げます。

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

unifyは単一化関数で

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

未束縛なunboundの中にレベルが含まれているので、occurs\_check\_adjust\_levelsに渡します。

          case (tvar @ TVar(Unbound(id, level)), ty) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)
これもそうです。

          case (ty, tvar @ TVar(Unbound(id, level))) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)

後は終わりです。

          case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
        }
      }

generalizeは一般化する関数でレベルを受け取ります。

      def generalize(level:level, ty:Ty):Ty = {
        ty match {

未束縛な型変数があったら、レベルが高い時だけ、一般化します。レベルが低い時は一般化しないことで単一化されなくなる事を防ぎます。ここがポイントです。

          case TVar(Unbound(id, other_level)) if other_level > level =>
            TVar(Generic(id))

他のTAppやTArrow,TVarのリンクは再帰的にレベルを引き渡します。

          case TApp(ty, ty_arg_list) =>
            TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
          case TArrow(param_ty_list, return_ty) =>
            TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
          case TVar(Link(ty)) => generalize(level, ty)
          case TVar(Generic(_)) | TVar(Unbound(_, _)) | TConst(_) => ty
        }
      }

intantiateは具体化する関数でここもlevelを受け取ります。

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

型変数を作るときにlevelを保存するだけです。

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

match\_fun\_tyは関数の型のマッチングを行うinfer関数の補助関数です。

      def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
        ty match {
          case TArrow(param_ty_list, return_ty) =>
            if (param_ty_list.length != num_params)
              error("unexpected number of arguments")
            (param_ty_list, return_ty)
          case TVar(Link(ty)) => match_fun_ty(num_params, ty)

未束縛な型変数の時は、レベルを取り出して、引数とリターン値用の新しい型変数をレベル付きでつくります。

          case tvar@TVar(Unbound(id, level)) =>
            val param_ty_list = List.fill(num_params){new_var(level)}
            val return_ty = new_var(level)
            tvar.a = Link(TArrow(param_ty_list, return_ty))
            (param_ty_list, return_ty)
          case _ => error("expected a function")
        }
      }

型推論のメインのinferもlevelを受け取ります。

      def infer(env:Map[String,Ty], level:level, expr:Expr):Ty = {
        expr match {
          case Var(name) =>
            try {

具体化するときにlevelを渡し

              instantiate(level, env(name))
            } catch {
              case _:Throwable => error("variable " + name + " not found")
            }
          case Fun(param_list, body_expr) =>

型引数用にレベル付きで型変数を作り

            val param_ty_list = param_list.map{ _ => new_var(level)}
            val fn_env =
              param_list.zip(param_ty_list).foldLeft(env) {
                case(env, (param_name, param_ty)) => env + (param_name -> param_ty)
              }

型推論呼び出す時もレベル付きです。

            val return_ty = infer(fn_env, level, body_expr)
            TArrow(param_ty_list, return_ty)
          case Let(var_name, value_expr, body_expr) =>

Letのときは型推論呼び出す時にレベルを上げて呼び出します。

            val var_ty = infer(env, level + 1, value_expr)

一般化するとき

            val generalized_ty = generalize(level, var_ty)

型推論呼び出すとき

            infer (env + (var_name -> generalized_ty), level, body_expr)
          case Call(fn_expr, arg_list) =>

型推論呼び出すとき

            val (param_ty_list, return_ty) =
              match_fun_ty(arg_list.length, infer(env, level, fn_expr))
            param_ty_list.zip(arg_list).foreach{

型推論呼び出すときもレベル付きで呼び出します。

              case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
            } 
            return_ty
        }
      }
    }

## レベルを使ったプログラムの処理のまとめ

レベルを使った型推論のプログラムでは以下のような処理を行っています。

- new_varで新しい型変数を作る時はlevelを保存します。
- 新しい型変数は、関数のinferとmatch\_fun\_ty、instantiateでのみ作ります。
- inferはレベルを受け取り、呼び出すinfer,intantiate,generalize関数にレベルを引き渡し、スコープを作るletの最初の式のときはレベルを１つあげます。関数の引数の型変数にレベルを保存します。
- match\_fun\_tyの関数呼び出しの未束縛な型変数に含まれているレベルは取り出して型変数を作る時に使います。
- intantiateでは一般化されている変数を具現化するときに型変数にlevelを保存します。
- generalizeは未束縛な型変数があったら、型変数のレベルが現在のスコープレベルより高い時だけ一般化します。
- unifyは未束縛な変数中のレベルを取り出して、occurs\_check\_adjust\_levelsに渡し、
occurs\_check\_adjust\_levelsでは、未束縛な型変数が現れたらレベルをunifyから受け取った物に下げます。

## 発展について

Olegの解説ではさらに、最適な処理を遅延化してまとめるアルゴリズムがある訳ですが、そこまではこの例では扱っていません。

この先の最適化はインスタンス化中に単相型のトラバースを避けて速くします。
また、現れる型変数の最大型レベルでの型レベルのすべての型をマークするみたいです。ちょっとここはよくわからないのですけど。
また、再帰的な出現のチェックはまとめて遅延すると速くなるということみたいです。

ここの文章はまだ翻訳しきれてないので、いい加減になってしまってます。

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
