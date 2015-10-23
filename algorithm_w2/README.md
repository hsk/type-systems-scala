# さらに効率的なレべルべースの​​一般化


ここでは、Olegの説明の翻訳だと良くわからなかったので意訳しまくって分かりやすくしてみようと思います。
とにかく、algorithm wをより強化した レミーのsound_lazyアルゴリズムを見て行きます。

sound_lazyは、無駄な単一化時の型チェックの不必要なトラバースをなくし、
一般化とインスタンス化の繰り返しをなくします。

さらに、一般化またはインスタンス化する変数が含まれてない部分をコピーして共有することで高速化します。
型変数の単一化は重いので出現チェックとレベルの更新を遅延させます。
レベルは必要になってからインクリメンタルに更新されます。

# 構文木の型を変更する。

まず、構文木の型を変えましょう。

ところでレベルはネストの深さを表します。ネストが深ければ深い程値が大きくなります。
レベルが高い、低いというと、ネストの深さのレベルなので混乱します。
従ってここでは、高い低いと書かずに、深い、浅いと記述します。

## test2.ml 一般化変数を無くす

一見乱暴ですが、一般化型変数の要素をなくして一般化レベル(generic_label)という(地球の中心部には行けないくらい深い)巨大な値を作ります。
そして、generic_levelがlevelに設定してあるUnboundな変数は一般化された型変数として扱います。
コレは簡単ですね。

## test3.ml repr関数を使ってリンクを出来るだけ短くする

    let rec repr : typ -> typ = function
      | TVar ({contents = Link t} as tvr) -> 
          let t = repr t in
          tvr := Link t; t
      | t -> t

補助関数のreprでは、自由変数または構築された型を返すバインド変数のリンクを追います。
reprは至る所で使われています。

## test4.ml 全ての型がレベルを持つようにする

次に、全ての型が今いるレベルを持つようにします。
といっても型変数以外には関数の型しかないので、関数の型にもレベルを持たせます。

表層部分の型が生きていなら、中味の型も生きているので中味を見る必要はないので、
関数の型の中にある型のレベルは、必ず今のレベルが設定されているわけではありません。

また、関数の型がgeneric\_levelなら、量化された型変数を含んでいます。
逆に、関数の型がgeneric\_levelでないなら、量化変数は含まれていません。

関数の型がgeneric\_levelでない型のインスタンスを作成したばあいトラバースせずにそのまま型を返します。
関数の型のレベルが現在レベルよりも深い場合は、一般化される型変数があるかもしれません。

一般化関数でも、そのレベルが同じか、現在よりも浅い型をわざわざトラバースする必要はありません。 レベルを共有することで、型のトラバースと再生成をなくすことができるので高速化できます。

## 3. 単一化時のoccurs チェックの遅延とold_level

型変数のレベルが浅い場合は別の型tと単一化すると、型変数のレベルにtのレベルを更新する必要があります。 
関数の型の場合は、再帰的に関数内の型のレベルを更新する必要がありますが、
トラバースは重いので内部の型の更新処理を遅延させます。

関数の型には最深レベルを表すlevel\_oldと、内部のレベルを持つlevel\_newの２つのレベルがあります。

level\_new < level\_oldならレベルの更新を必要になるまで遅延させます。

従って型の構文は以下のようになります：

```
type level = int
let generic_level = 100000000           (* OCaml の typing/btype.ml のようにする *)
let marked_level  = -1                    (* for marking a node, to check*)
                                        (* for cycles                  *)
                                        (* ノードをマークするための   *）
                                        (* サイクルのチェック *）

type typ =
  | TVar of tv ref
  | TArrow of typ * typ * levels
and tv = Unbound of string * level | Link of typ
and levels = {mutable level_old : level; mutable level_new : level}
```

## 4. marked\_level 出現チェックトラバース確認済みマーク用レベル

unifyのときに毎回する出現チェックをする場合は、代入する型に自分がいるかどうかをチェックしていたので、再入のある型は存在しませんでした。

型変数との単一化で毎回出現チェックをすると重くなるので出来るだけ、関数同士のunifyや一般化時、式全体の型チェック終が終わった後まで、出現チェックを遅延します。

こうすると、再入チェック無しに代入操作を行うので型の中に再帰的な構造が存在してしまいます。

一般的に、再入のあるデータをトラバースする場合トラバース済みである事を示すマークアンドスイープのマークビットのようなものがないと無限ループに陥ります。

出来るだけ無駄なデータは持ちたくないので、このアルゴリズムではlevel_newにトラバース済みのマークを付けます。

marked\_levelは型のトラバース中である事を示し、level_newに一時的に設定されます。

トラバース中にlevel\_newを一時変数に保存し、level\_newにmark\_levelを設定し、内部の型をトラバースし、
終わったら、一時変数からlevel\_newを戻すわけです。

出現チェック時にmarked\_levelがあったら、再帰があることになるので、出現チェックエラーを発生させます。

mark_levelはマイナス値なので、お掃除中は一度ベットの上に乗っける感じですね。
たこ足配線しまくったマルチタップのコードを辿るのに、ベットの上に置いて行く事にして辿って行ったら乗っかっているタップに繋がってたみたいな話です。これでは電気が来る訳がない！っていう。


--------------


## unify関数

このバージョンではパスの圧縮を行います。
unifyでは、出現チェックを行わないので、出現チェックは後でやらないと行けません。

    let rec unify : typ -> typ -> unit = fun t1 t2 ->
      if t1 == t2 then ()                   (* t1 and t2 are physically the same *)
      else match (repr t1,repr t2) with
      | (TVar ({contents = Unbound (_,l1)} as tv1) as t1,      (* unify two free vars *)
        (TVar ({contents = Unbound (_,l2)} as tv2) as t2)) ->
          if l1 > l2 then tv1 := Link t2 else tv2 := Link t1  (* bind the higher-level var *)

型変数のunifyは、deepなoccursチェックではなくshallowな update_levelをした変数をバインドします。

      | (TVar ({contents = Unbound (_,l)} as tv),t')
      | (t',TVar ({contents = Unbound (_,l)} as tv)) ->
          update_level l t';
          tv := Link t'

二つの関数の型の単一化はすぐに型をトラバースして、レベルの更新を保留しません。

      | (TArrow (tyl1,tyl2,ll), TArrow (tyr1,tyr2,lr)) ->
          if ll.level_new = marked_level || lr.level_new = marked_level then
            failwith "cycle: occurs check";
          let min_level = min ll.level_new lr.level_new in
          ll.level_new <- marked_level; lr.level_new <- marked_level;
          unify_lev min_level tyl1 tyr1;
          unify_lev min_level tyl2 tyr2;
          ll.level_new <- min_level; lr.level_new <- min_level
      (* everything else is the unification error *)

    and unify_lev l ty1 ty2 =
      let ty1 = repr ty1 in
      update_level l ty1;
      unify ty1 ty2

## update_level関数

update_levelは重要な関数です。

    let to_be_level_adjusted = ref []

    let update_level : level -> typ -> unit = fun l -> function

大体の場合はupdate_level関数はレベルを更新するだけで後で出現検査する約束をして終わります。
型変数のレベルの書き換えはすぐに終わります。

      | TVar ({contents = Unbound (n,l')} as tvr) -> 
          assert (not (l' = generic_level));
          if l < l' then
            tvr := Unbound (n,l)

関数の型の場合は

      | TArrow (_,_,ls) as ty -> 
          assert (not (ls.level_new = generic_level));
          if ls.level_new = marked_level then failwith "occurs check";

          if l < ls.level_new then begin

レベルが浅い場合、

変更前のlevel\_newとlevel\_oldが同じだった場合、後でレベルを更新するために、型はto\_be\_level\_adjustedキューに入れます。

            if ls.level_new = ls.level_old then
              (*遅延用リストに加える*)
              to_be_level_adjusted := ty :: !to_be_level_adjusted;

この作業キューは、世代別ガベージコレクタの古い世代から若い世代へリンクのリストに似ています。

最後にlevel\_newを新しいレベルにします。

            ls.level_new <- l
          end
      | _ -> assert false

--------------

## get_level

get_level関数は型からレベルを取得します。

    (* 型からレベル取得 *)
    let get_level : typ -> level = function
      | TVar {contents = Unbound (_,l)} -> l
      | TArrow (_,_,ls)                 -> ls.level_new
      | _ -> assert false

## force\_delayed\_adjustments

後でやるリスト中の仕事は、一般化する前にやらないと行けません:
force\_delayed\_adjustments関数がその仕事をします。
でも、まとめてやるリストに溜め込んだおかげで、型変数のレベルが下がると使ってない場合は使わないんだから仕事する必要が無くなります。


    let force_delayed_adjustments level =

adjust_oneはキュー内の型をトラバースするメイン関数です。

      let rec adjust_one acc = function

level_oldよりレベルが低い場合は型は現時点での一般化なしの変数がないのでまた後で処理します。

        | TArrow (_, _, ls) as ty when ls.level_old <= level ->
            ty::acc                         (* update later *)

同じ場合は処理済みなのでキューからデータを消します。

        | TArrow (_, _, ls) when ls.level_old = ls.level_new ->
            acc                             (* already updated *)

残念ながらlevel\_old > current\_levelを持つ型の仕事はしないといけません。

        | TArrow (ty1, ty2, ls) ->

レベルをlevel\_newから取り出して、level\_newには検査済みのマークを付けて、２つの型を検査します。

            let level = ls.level_new in
            ls.level_new <- marked_level;
            let acc = loop acc level ty1 in
            let acc = loop acc level ty2 in

終わったら、検査中マークを元に戻し、oldもnewにあわせる事で、処理済みにします。

            ls.level_new <- level;
            ls.level_old <- level; 
            acc
        | _ -> assert false

ループ関数は、

      and loop acc level ty = 
        match repr ty with

レベルがlevelより大きければ

        | TVar ({contents = Unbound (name,l)} as tvr) when l > level ->

levelに更新します。

            tvr := Unbound (name,level); acc

level\_newがmarked\_levelならエラーです。

        | TArrow (_,_,ls) when ls.level_new = marked_level ->
            failwith "occurs check"

関数なら

        | TArrow (_,_,ls) as ty ->

level\_newがlevelより大きければ、level\_newにlevelを設定します。

            if ls.level_new > level then ls.level_new <- level;

そして中味をさらにチェックします。

            adjust_one acc ty
        | _ -> acc
      in
      to_be_level_adjusted :=
        List.fold_left adjust_one [] !to_be_level_adjusted

## gen関数

一般化関数は死んでいるリージョンに属する（つまり、そのレベルが現在よりも深い）TVarsを検索し、彼らのレベルをgeneric\_levelに設定することで、変数を量化します。

gen関数は一般化する型変数を含むことができる型の部分だけトラバースします。

型はcurrent\_level以下の（新しい）レベルであれば、すべてのコンポーネントは生きているがゆえに型は一般化に関係しません。

一般化した後、量化された型変数がある場合、関数の型はgeneric\_levelになります。

その後、インスタンス化関数は、generic\_levelのレベルである型だけ見る事になります。

```
let gen : typ -> unit = fun ty ->
  force_delayed_adjustments ();
  let rec loop ty =
    match repr ty with
    | TVar ({contents = Unbound (name,l)} as tvr)
      when l > !current_level -> tvr := Unbound (name,generic_level)
    | TArrow (ty1,ty2,ls) when ls.level_new > !current_level ->
      let ty1 = repr ty1 and ty2 = repr ty2 in
      loop ty1; loop ty2;
      let l = max (get_level ty1) (get_level ty2) in
      ls.level_old <- l; ls.level_new <- l   (* set the exact level upper bound *)
              (*上限正確なレベルを設定します*)
    | _ -> ()
in loop ty
```


## infer

型チェッカーのinferはlet式を型チェックするときにそのまま新しいリージョンに入ります。

詳細については、ソースコードを参照してください。


## まとめ

ここでは、一般化で全体の型環境を最適化されたsound\_lazy型一般化アルゴリズムを見ました。

1. 重い型変数の単一化で、毎回単一化するたびに出現チェックする事を避けます。
2. 不要な型トラバーサルやコピーを排除してメモリ効率も上げ高速化します。

二つのアイデアは型変数の型レベルに加えて、最適化の基礎となります。

一つ目のアイディアは関数の型にレベルを保存するだけにして、中味を見なくします。

二つ目のアイディアは別の物とまとめて後で計算出来ることを期待して、コストの高いアクション（型トラバーサル）を遅延させています。

要するに遅延評価戦略のおかげで、場合によっては計算しなくて済むかもしれないのです。

#### References
#### 参考資料

[sound_lazy.ml](http://okmij.org/ftp/ML/generalization/sound_lazy.ml) [18K]

最適化されたオモチャの型推論の動作するコードと、また沢山の例

あ
⭕⭕⭕🔵⭕⭕⭕⚫⭕⭕⭕🔵⭕⭕⭕
⭕🔵⭕🔴⭕🔵⭕⚫⭕🔵⭕🔴⭕🔵⭕️
⭕🔵⭕🔴⭕🔵⭕️⚫⭕🔵⭕🔴⭕🔵⭕
⭕⭕⭕🔵⭕⭕⭕⚫⭕⭕⭕🔵⭕⭕⭕
あ


⚫⭕⭕⭕🔵⭕⭕⭕⚫⭕⭕⭕🔵⭕⭕⭕⚫⭕⭕⭕🔵a
⭕⚫🔵⭕⭕⚫🔵⭕⭕️⚫🔵⭕⭕⚫🔵️⭕⭕⚫🔵⭕⭕a
🔵⭕⚫⭕🔵⭕⚫⭕🔵⭕⚫⭕🔵️⭕⚫⭕🔵️⭕⚫⭕🔵a
⭕⭕🔵⚫⭕⭕🔵⚫⭕⭕🔵️⚫⭕⭕🔵⚫⭕⭕🔵⚫⭕a
🔵⭕⭕⭕⚫⭕⭕⭕🔵⭕⭕⭕️⚫⭕⭕⭕🔵⭕⭕⭕⚫️a

●○○○◎○○○●
○●◎○○●◎○○●
◎○●○◎○●○◎○●
○○◎●○○◎●○○◎●
◎○○○●○○○◎○○○●

う

⭕⭕⭕⭕🔴⭕⭕⭕⭕
⭕⭕🔵⭕🔴⭕🔵⭕⭕️
🔵⭕⭕⭕🔴⭕⭕⭕🔵️
⭕⭕🔵⭕🔴⭕🔵⭕⭕️
⭕⭕⭕⭕🔵⭕⭕⭕⭕

あ
⭕⭕⭕⭕🔴⭕⭕⭕⭕
⚫⭕🔵⭕⭕🔴🔵⭕⭕⭕️
⚫⚫🔵⭕⭕⭕🔴⭕🔵⭕⭕️
⚫⚫⚫⭕🔵⭕⭕🔴⭕⭕🔵⭕️
⚫⚫⚫⚫⭕⭕🔵⭕🔴⭕⭕⭕🔵
⚫⚫⚫⚫⚫⭕⭕⭕🔵🔴🔵⭕⭕⭕
う

⭕⭕⭕⭕⭕⭕🔴⭕⭕
⚫⭕⭕⭕🔵⭕⭕🔴🔵⭕
⚫⚫🔵⭕⭕⭕⭕⭕🔴⭕🔵
⚫⚫⚫⭕⭕⭕🔵⭕⭕🔴🔵⭕️
⚫⚫⚫⚫⭕⭕⭕⭕⭕⭕🔵⭕⭕️

