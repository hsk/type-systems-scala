# Polymorphism, subtyping and type inference in MLsub の適当な訳

元の論文は以下のURLから手に入ります:

https://c10109cf-a-62cb3a1a-s-sites.googlegroups.com/site/mlworkshoppe/polymorphism-subtyping-and-type-inference-in-mlsub.pdf

> <sup><sub>|
superset|上位集合|
parametric|パラメトリック|
</sub></sup>

> We present a type system combining subtyping and ML-style parametric polymorphism.

> 我々は提示する型システムをまとめたものサブタイピングとMLスタイルパラメトリック多相性。

我々はサブタピイングとMLスタイルパラメトリック多相性をまとめた型システムを提示する。

> Unlike previous work, our system supports type inference and infers compact types.

> 似てない以前の仕事、我々のシステムはサポートする型推論と推論コンパクトタイプ。

以前の仕事との違いは、我々のシステムは型推論と推論コンパクトタイプをサポートしている事である。

> We demonstrate this system in the minimal language MLsub, which types a strict superset of core ML programs.

> 我々はデモするこのシステム内で最小言語MLsub、                  これは型付け厳格なスーパーセットおぶコアMLプログラム。

我々はこのシステム内でコアMLプログラムの厳格なスーパセットで型付けする、最小言語MLsubをデモする。

## Introduction

### Introduction 1

> <sup><sub>|
descendants|子孫|
practical|実用的な|
sporting|スポーツの、公正な、変種の、勝負事|
decidable|決定可能な|
</sub></sup>


> The Hindley-Milner type system of ML and its descendants is popular and practical, sporting decidable type inference and principal types.

> ヒンドリミルナ型システムおぶMLとこれらの子孫は人気がありかつ実用的、変種は決定可能だ型推論と実用的な型。

MLとこれらの子孫のヒンドリミルナ型システムは人気があり実用的で、決定可能な型推論と実用的な型をもつ変種である。

> <sup><sub>|
extending|延長|
preserving|保存|
problematic|問題|
</sub></sup>

> However, extending the system to handle subtyping while preserving these properties has been problematic.

> しかしながら、延長であるシステムからハンドルするサブタイプを、間、保存これらのプロパティが持っている問題を。

しかしながらこれらのプロパティが持っている問題を保持している間、サブタイピングを延長であるシステムからハンドルする。

### Introduction 2

> <sup><sub>|
but also|だけでなく|
expose|暴露する、公開する|
such|そのような|
carefully|慎重に|
Consider|検討して|
predicate|述語|
</sub></sup>


> Subtyping is useful to encode extensible records, polymorphic variants, and object-oriented programs,
but also allows us to expose more polymorphism even in core ML programs that do not use such features by more carefully analysing data flow.

> サブタイピングは使いやすいからエンコード拡張可能レコード、多相的バリアント、そしてオブジェクト指向プログラム、
しかしながら許す我々にとぅ公開するもっと多相的なイーブンいんコアMLプログラムだっと違うつかうようなおぶ機能ばいもっと慎重に調べろデータフロー。

> google: サブタイプは、拡張可能な記録、多型変異体、及びオブジェクト指向プログラムをエンコードすることが有用なだけでなく、私たちはもっと慎重にデータフローを分析することによって、このような機能を使用していないコアMLプログラムでより多くの多型を公開することができます。


拡張可能なレコード、多相的バリアント、オブジェクト指向プログラミングからサブタイプはエンコードしやすいが、しかしデータフロー解析をもっと慎重にデータフローを分析してから、機能を使わないコアMLプログラム内でより多くの多相型を公開する事を許可する。

> Consider the select function, which takes three arguments: a predicate p, a value v and a default d, and returns the value if the predicate holds of it, and the default otherwise:

> 検討してselect関数、これ取る３つの引数：述語p,値vとデフォルトd、そして返す値もし述語保持するおぶこれ、そしてデフォルト他の：

> google: 述語はそれを保持している場合、値を述語p、値VとデフォルトのD、および返し、それ以外の場合は、デフォルト：3つの引数をとり選択機能を、考えてみましょう：


以下のselect関数を検討しよう。この関数は述語p,値vとデフォルトdの３つの引数を取る。もしこの術語が保持される値を返し、そうでなければデフォルト値を返す:


	select p v d = if (p v) then v else d

> In ML and related languages, select has type scheme

> いんMLと最近の言語、select持ってるタイプスキーム

MLや最近の言語では、selectはタイプスキームを持つ

	(α → bool) → α → α → α


### Introduction 3

> <sup><sub>|
strange|奇妙な|
demands|要求|
whatever|どのような|
acceptable|許容できる|
constraint|制約|
arise|生じる|
behaviour|ふるまい|
</sub></sup>


> This type is quite strange, in that it demands that whatever we pass as the default d be acceptable to the predicate p.

> この型はかなり奇妙、いんだっとこれ要求だっとどのような我々通すあずザデフォルトdは許容出来るとぅざ述語p.

術語pへ許可出来るデフォルトdを返すような要求の中で、この型はかなり奇妙である。

> But this constraint does not arise from the behaviour of the program: at no point does our function pass d to p.

> しかしこれ制約するそんなことない生じるフロムザ振る舞いおぶザプログラム：あっとない点だず我々の関数通すdからp。


しかしこの制約は我々の関数がdからpを通すというプログラムの振る舞いから生じることはない。


### Introduction 4

> <sup><sub>|
examine|調べます|
actual|実際|
</sub></sup>

> Let’s examine the actual data flow of this function:

> レッツ調べるザ実際データフローおぶこの関数:

この関数の実際のデータフローを調べよう:

	arguments to p <- v -> result <- d

> <sup><sub>|
ignoring|無視する|
orientation|向き|
edges|エッジ|
conclude|結論を出す|
</sub></sup>

> Only by ignoring the orientation of the edges above could we conclude that d flows to the argument of p.

> オンリーバイ無視するザ向きおぶざエッジ上記の出来る我々結論を出すざっとdフローとぅざ引数おぶp.

> google:唯一のエッジの向きを無視することによって上記の我々は、Dは、pの引数に流れていることを結論付けることができます。


上記のエッジの向きを無視することのみで我々はpの引数からdが流れていると結論付ける事が出来る。

> <sup><sub>|
Indeed|確かに|
exactly|正確に|
equality|平等|
constraints|制約|
direction|方向|
</sub></sup>

> Indeed, this is exactly what ML does: by turning data flow into equality constraints between types, information about the direction of data flow is ignored.

> 確かに、これは正確だ何MLだず：ターニングデータフローからタイプ間で等価制制約内で、

> データフローの方向の情報ついて無視する。

> google: 確かに、これはMLはまったく同じものです：型の間で等式制約にデータフローを回すことにより、データフローの方向についての情報は無視されます。

たしかに、これはMLについて正確だ:
タイプ間で等価性制約内でデータフローから回す事で、データフローの方向の情報ついて無視する。

> <sup><sub>|
Since|以来、いるので|
equality|平等|
treated|治療|
undirected|無向|
</sub></sup>

> Since equality is symmetric, data flow is treated as undirected.

> なぜならば等価性は対比的、データフローは整えらえたあず無方向。

なぜならば等価性は対比的で、データフローは無方向で整えられているからだ。

### Introduction 5

> To support subtyping is to care about the direction of data flow.

> 部分型サポートはデータフローの方向についてケアする事である。

> google:サブタイプをサポートするには、データフローの方向を気にすることです

部分型サポートはデータフローの方向について気にする事だ。

> <sup><sub>|
provide|提供します|
least|最低|
guarantees|保証|
destination|宛先|
</sub></sup>

> With subtyping, a source of data must provide at least the guarantees that the destination requires, but is free to provide more.

> サブタイピングでは、ソースおぶデータ必要提供するあっと最低保証だっと先に必要とする、しかし自由とぅ提供するもっと。

部分型付けでは、データのソースは宛先を必要とするが、たくさん提供する事で自由であることを最低限保証する必要がある。

### Introduction 6

> <sup><sub>|
noticed|気づいた|
separation|分離|
bipartite|二分|
cycles|サイクル|
</sub></sup>

> In his PhD thesis, Pottier1 noticed that the graph of data flow has a simple structure.

> インかれのドクター論文、Pottier1気がつくざっとざグラフおぶデータフローもってるシンプル構造。

彼のDr.論文中で、Potterはデータ構造が持っているシンプルな構造について気がついている。

> By keeping a strict separation between inputs and outputs, we can always represent the constraint graph as a bipartite graph: data flows from inputs to outputs.

> ばい保持構造分離、間でインプットとアウトプット、我々出来るすべてで表現ざ制限グラフあず２分木グラフ：データフローふろむ入力とぅ出力。

入力と出力の間の構造を分ける事で、我々は入力から出力へのデータフローを全て２分木のグラフで制限を表現出来る。

> With edges only from inputs to outputs, such graphs have no cycles (or even paths of more than one edge), simplifying analysis.

> ありでエッジのみふろむインプットからアウトプット、そのようなグラフ持ってるノーサイクル(またはイーブンパスおぶもっとざん１つのエッジ)、簡単化解析

入力から出力へのエッジでは、サイクル無しのグラフ(もしくは１つのエッジ以上のパスを持っている)のようなものがある。

### Introduction 7

> <sup><sub>|
insight|洞察力|
further|さらにまた|
religious|宗教的な|
distinction|違い|
</sub></sup>

> We take this insight a step further,

> 我々取るこの洞察力段階的さらにまた、

我々はこの洞察から次のステップを取り、

> and show that by keeping the same religious distinction between input and output we can develop a variant of unification compatible with subtyping,

> そしてみるザットバイ保持ザ同じ宗教的な違い間入力と出力我々出来る開発バリアントおぶ単一化コンパチブルうぃす部分的構造化、

> allowing us to infer types.

> 許可我々から推論型。

> google: 私たちは、さらに一歩、この洞察を取り、入力と出力の間で同じ宗教の区別を維持することによって、我々は、私たちは型を推測できるように、サブタイプと互換性の統一のバリアントを開発できることを示しています。

そして同じ宗教的な入力と出力の間の違いを維持する事で、我々は型推論を許可するサブタイピングでのコンパチブルな単一化のバリアントを開発できることを示しています。


# 2 Input and output types

> <sup><sub>|
form|形成する|
lattice|格子|
least-upper-bound|最小上限|
greatest-upper-bound|最大上限|
appear|現れます|
arbitrarily|任意|
randomly|無作為に|
produce|作り出す|
either|どちらか|
actual|実際|
</sub></sup>


> Our types form a lattice, with a least-upper-bound operator ⨆ and a greatest-upper-bound operator ⨅.

> 我々の型は形成する格子を、最小上限演算子⨆そして最大上限オペレータ⨅.

> google: 私たちのタイプは、最小-上限オペレータ⨆と最大-上限オペレータ⨅で、格子を形成します。

> ※訳注: formはfromではなかったw formは動詞

我々の型は最小上限演算子⨆と最大上限オペレータ⨅で格子を形成します。

> The structure of programs does not allow the lattice operations ⨆ and ⨅ to appear arbitrarily.

> 構造おぶプログラム許可しない格子⨆と ⨅演算子が現れる事を任意の

> google:プログラムの構造は、格子の操作を許可していません⨆と⨅任意に表示されるように。

プログラムの構造は任意の⨆と⨅の格子の操作の存在許可していません。

> If a program chooses randomly to produce either an output of type τ1 or one of type τ2, the actual output type is τ1 ⨆ τ2.

> もしも、プログラムが選ぶランダムに生成することどちらかオブジェクトおぶ型τ1か型τ2の1つ、実際の出力タイプはτ1 ⨆ τ2です。

> google: プログラムは、型τ1の出力またはタイプτ2のいずれかを生成するためにランダムに選択した場合、実際の出力タイプは、τ1⨆τ2です。

もしもプログラムがランダムに選んで生成されたτ1か型τ2の1つのうちどちらかのオブジェクトの場合、実際の出力型はτ1 ⨆ τ2です。


> <sup><sub>|
Similarly|同様に|
again|再び|
arises|生じる|
describing|記載する|
</sub></sup>

> Similarly, if a program uses an input in a context where a τ1 is required and again in a context where a τ2 is, then the actual input type is τ1 ⨅ τ2.

> 同様に、もしもプログラムが使う入力いん文脈フェアτ1が必要だったあるいは、またいん文脈フェアτ2、ならば実際の入力型はτ1 ⨅ τ2です。

> google: τ2がどこにあるプログラムはτ1がコンテキストで再び必要とされるコンテキスト内で入力を使用する場合も、実際の入力タイプは、τ1⨅τ2です。


同様に、もしもプログラムが使う入力が文脈中でτ1が必要だったあるいは文脈τ2が中にある、ならば実際の入力型はτ1 ⨅ τ2です。

> Generally, ⨆ only arises when describing outputs, while ⨅ only arises when describing inputs.

> 一般的に、出力に記載されたときのみが⨆発生し、入力中に記載された時のみ⨅が発生する。

> google: 一般的に、出力を記述する際のみ、⨆発生し、入力を記述する際⨅間だけ発生します。

一般的に、出力に記載されたときのみが⨆発生し、入力中に記載された時のみ⨅が発生する。

> <sup><sub>|
similar vein|同じような文脈|
Thus|このように|
distinguish|区別|
describe|説明する|
</sub></sup>


> In a similar vein, the least type ⊥ appears only on outputs (of non-terminating programs), while the greatest type ⊤ appears only on inputs (an unused input).

> このような文脈の中で、(非端末プログラムの)出力上にのみ最小限の型⊥があらわれ、(使用していない入力)入力上にのみ上限型⊤が現れる。

> google: 最大のタイプは⊤のみ（未使用の入力）入力に表示されている間同じような文脈では、少なくともタイプが⊥、（非終端プログラムの）のみの出力に表示されます。

このような文脈の中で、(非端末プログラムの)出力上にのみ最小限の型⊥があらわれ、(未使用の入力)入力上にのみ上限型⊤が現れる。

> Thus, we distinguish positive types τ+ (which describe outputs) and negative types τ− (which describe inputs):

> このように、我々は区別しますポジティブ型τ+(出力で説明する)とネガティブ型τ−(入力で説明する)を:

> google: したがって、我々は、（入力を記述）は、正タイプτ+（出力を記述ている）と負の種類τ-を区別：

このように、我々は(出力に記述される)ポジティブ型τ+と(入力に記述される)ネガティブ型τ−を区別します:

	τ+ ::= α | τ+ ⨆ τ+ | ⊥ | unit | τ− → τ+ | µα.τ +
	τ− ::= α | τ− ⨅ τ− | > | unit | τ+ → τ− | µα.τ −

> Positive types describe something which is produced, while negative types describe something which is required.

> ポジティブ型は記述します何かをどこかで生成するときに、のときはネガティブ型は記述します何かをどこかで必要とされているときに。

> google: ポジティブタイプは、生成されたものを説明し、負のタイプは何かを記述しながら、必要とされます。

ポジティブ型はどこかで何かを生成するときに記述され、ネガティブ型はどこかで何かが必要されているときに記述されます。

## 3 Unification and biunification

### 3.1

> <sup><sub>|
relies|依存している|
dealing|取扱う|
equations|方程式|
deal|契約|
constraints|制約|
rather|むしろ|
equations|方程式|
</sub></sup>

> The core operation of the Damas-Milner type inference algorithm [1] is unification.

> コアの操作おぶDamas-Milner型推論アルゴリズム[1]は単一化です。

Damas-Milner型推論アルゴリズム[1]のコアの操作は単一化です。

> Unification relies on the substitution of equals for equals, which maps well to dealing with systems of equations between types.

> 単一化は同じものを同じ物に代入する事に依存していて、これはマップされるよく型の間の方程式のシステムの取り扱いが

> google: 統一は、タイプ間の方程式のシステムを扱うによくマッピングするための対等対等の置換に依存しています。

単一化は同じ型を同じ型に代入する事に依存していて、これは型の間の方程式のシステムを取り扱うのによくマッピングされている。


> With subtyping, the standard unification algorithm does not apply, since we deal with subtyping constraints rather than type equations.

> サブタイピングでは、標準的な単一化アルゴリズムは実行しません、なぜならば我々は契約していますサブタイピング制約がむしろ型の方程式がるので

> 我々はサブタイプの制約ではなく、型の方程式を扱うので、サブタイプで、標準統一アルゴリズムは、適用されません。

サブタイピングでは、我々はサブタイピングの制約ではなく型の方程式を扱うので、標準的な単一化アルゴリズムは適用しません。

### 3.2

> <sup><sub>|
for instance|たとえば|
dual|の二重|
</sub></sup>

> There are three different situations in which DamasMilner inference uses unification. 

> シチュエーションDamasMilner型推論が使う単一化中には３つの違いがあります。

> google:DamasMilner推論が統一を使用する3つの異なる状況があります。

DamasMilner型推論が単一化を使う中で３つの異なる状況があります。


> The first is to unify two possible output types of an expression, for instance the two branches of an if-expression.

> 最初の１つは単一化すること２つの可能な出力型おぶ式、ための実体の２つのブランチおぶif式。

> google:最初は、例えば、もし式の二つの分岐を表現可能な2つの出力タイプを統一することです。

1つめはif式の２つの分岐の式の２つの可能な出力型を単一化することです。

> The second is the dual of the first, unifying two required input types of an expression when typing a λ-bound variable (all uses of which must be at the same type). 

> ２つ目は最初と重なりますが、単一化すること２つの必要な入力おぶ式 型がλ束縛された値のとき(必要な同じ型は全て使う)

> google:第二は、λ-バインド変数を（のすべての使用は、同じタイプでなければなりません）と入力したときに、式の2つの必要な入力タイプを統一、最初のデュアルです。

２つ目は最初と重なりますが、型がλ束縛変数(使う型は全て同じ型)のときの２つの必要な入力の式を単一化することです。

> <sup><sub>|
correspond|対応します|
respectively|それぞれ|
disparate|異種|
underconstrained|拘束します|
useless|役に立ちません|
disparate|異種|
overconstrained|制約を超えます|
neither|どちらも|
can cause|発生する可能性がある|
</sub></sup>

> With subtyping, these correspond respectively to the introduction of a ⨆ or a ⨅ operator. 

> サブタイピングでは、これらはそれぞれ⨆ または ⨅の操作の表現に対応します。

> google:サブタイプで、これらは⨆の導入または⨅オペレータにそれぞれ対応します。

サブタイピングでは、これらはそれぞれ ⨆ または ⨅ の操作の表現に対応します。


In MLsub, these cannot fail: an `if` which may produce two disparate types produces an underconstrained and useless output, an a λ-bound variable used in two disparate ways requires an overconstrained and impossible input, but neither can cause an error.

> MLsubでは、これらは失敗できません: もしフィッチもしも生成する２つの異種の型、生成する拘束されそして使われない出力、とλ束縛された変使われたイン２つの異種の方法必要とする制約を超えるかつ不可能な入力、しかしどちらもエラーが発生する可能性があります。

> google: MLsubでは、これらは失敗することはできません: 2つの異なるの型を生成することができる`if`は、underconstrained無駄な出力を生成
2つの異なる方法で使用λ-バインド変数がoverconstrainedと不可能な入力を必要とし、どちらも、エラーが発生する可能性があります。

MLsubでは、これらは失敗できません:
2つの異なる型を生成する事が出来る`if`は拘束されたと使わない出力が生成され、
2つの異なる方法で使われるλ-バインド変数は制約をこえた不可能な入力を必要とし、
どちらも、エラーが発生することがあります。

### 3.3

> <sup><sub>|
For instance|たとえば|
routing|経路|
tends|期待して|
badly.|ひどいです。|
to be the same as|は同じです|
</sub></sup>

> The third situation in which unification is used is the routing of inputs to outputs.

> ３つ目の単一化が使われる状況は、入力から出力への経路です。

> google 統合が使用される第3の状況は、出力に対する入力の経路です。

３つ目の単一化が使われる状況は、入力から出力への経路です。

> For instance, the typing rule for an application e1 e2 constrains the type of the value produced by e2 to be the same as that required by the domain of e1.

> たとえば、型付けルールふぉあ適用 e1 e2 はつぎを制約する。 e2から生成される値の型は同じです(that e1のドメインから必要とされる )

> google:例えば、アプリケーションのE1、E2のための型付け規則は、E1のドメインで必要とされるものと同じになるようにE2によって生成される値の種類を制限します。

例えば、適用 e1 e2の型付け規則はe2から値生成された物とe1のドメインから必要とされた物は同じ型であると制限します。

> If they don’t match, this can cause an error:

> もしかれらがマッチしない場合、これはエラーを発生します:

もしこれらがマッチしない場合、これはエラーを発生します:

> passing a string to a function expecting an integer tends to end badly.

> 通過させる文字列とぅ関数 予期される整数　傾向 とぅ終わりが酷いです。

> google:整数を期待して関数に文字列を渡すことはひどく終了する傾向があります。

整数が予期される関数に対して文字列を渡す事は酷く終了する傾向にあります。

### 3.4

> <sup><sub>|
demand|要望|
domain|ドメイン|
ensure|確保|
</sub></sup>

> With subtyping, we demand only that the type of `e2` be a subtype of the domain of `e1`.

> サブタイピングでは、我々は要望する唯一の（`e2`の型が`e1`のドメインのサブタイプである事を）.

> google:サブタイプでは、我々は`e2`のタイプはe1``のドメインのサブタイプであることだけを求めています。

サブタイピングでは、我々は`e2`の型が`e1`のドメインのサブタイプである事だけを求めています.


> Given `e1 : τ1− → τ2+, e2 : τ3+`, we have the constraint `τ1+ ≤ τ3−`.

> `e1 : τ1− → τ2+, e2 : τ3+`が与えられたとき、我々は制約`τ1+ ≤ τ3−`を持ちます。

> Google: 与えられた`E1：τ1-→τ2+、E2：τ3+`、我々は制約`τ1+≤τ3-`を持っています。

`e1 : τ1− → τ2+, e2 : τ3+`が与えられたとき、我々は制約`τ1+ ≤ τ3−`を持ちます。

> In general, our constraints are always of the form `τ+ ≤ τ−`: we ensure that some value that we produce of type τ+ is acceptable in some context that requires τ−.

> 一般的に、我々の制約は常に`τ+ ≤ τ−`の形のものです: われわれは確保します(同じ値を(我々が型τ+の生成が許容出来る 同じコンテキストの中で(τ−が必要な)))

> google:一般的には、私たちの制約は、常にフォームのある`τ+≤τ-`：私たちは型τ+の生成いくつかの値がτ-を必要とするいくつかのコンテキストで許容可能であることを確認してください。

一般的に、我々の制約は全て`τ+ ≤ τ−`の形式のものです: 我々が同じコンテキストの中でτ−が必要な型τ+の生成が許容出来る同じ値をわれわれは確保します。

### 3.5

> <sup><sub>|
restriction|制限|
analagous|類推の|
dub|吹き替えのせりふを入れる|
remaining|残りの|
involving|関連します|
decomposed|分解された|
</sub></sup>

> This syntactic restriction allows us to define an algorithm analagous to unification which we dub biunification.

> この構文的な制限は許可します我々に定義する事を単一化の類推アルゴリズム(我々が置き換えるbiunification)。

> google:この構文上の制限は、私たちがbiunificationを割り当てる統一するアルゴリズムanalagousを定義することができます。

この構文的な制限は我々に我々が置き換えるbiunification単一化の類推アルゴリズムを定義する事を許可します。

> <sup><sub>|
exclude|除外|
</sub></sup>

> The difficult cases of `τ1 ⨅ τ2 ≤ τ3` and `τ1 ≤ τ2 ⨆ τ3` are excluded by construction, while the remaining cases involving lattice operations (`τ1 ≤ τ2 ⨅ τ3` and `τ1 ⨆ τ2 ≤ τ3`) are easily decomposed into smaller constraints.

> `τ1 ⨅ τ2 ≤ τ3` と `τ1 ≤ τ2 ⨆ τ3`の難しいケースは生成時に除外し、残りの関連する格子の操作(`τ1 ≤ τ2 ⨅ τ3` と `τ1 ⨆ τ2 ≤ τ3`)のケースは簡単に小さな制約に分解されます。

> google: 困難な場合`τ1⨅τ2≤τ3`と`τ1≤τ2⨆τ3`建設で除外され、格子の操作を伴う残りの症例しばらく（`τ1≤τ2⨅τ3`と`τ1⨆τ2≤τ3`）を簡単に小さく制約に分解されます。

`τ1 ⨅ τ2 ≤ τ3` と `τ1 ≤ τ2 ⨆ τ3`の難しいケースは生成時に除外し、残りの関連する格子の操作(`τ1 ≤ τ2 ⨅ τ3` と `τ1 ⨆ τ2 ≤ τ3`)のケースは簡単に小さな制約に分解されます。

### 3.6

> <sup><sub>|
broadly|広い意味|
We then|それから|
in place of|変わりに|
</sub></sup>

> We then infer types using a method broadly similar to Damas-Milner inference, with biunification in place of standard unification.

> 我々は(型を推論します使っているメソッド広い意味で似ているDamas-Milner推論)、標準的な単一化の変わりにbiunificationとともに

> google:それから、標準的な統一の代わりにbiunificationと、ダマ・ミルナー推論に広く同様の方法を用いて型を推測します。

それから、標準的な単一化の変わりにbiunificationとともに、広い意味でDamas-Milner推論と似ている方法で型を推論します。

## 4 Algebraic subtyping


### 4.1

> <sup><sub>|
Much|多くの|
previous work|以前の研究|
containing|含みます|
quantification|定量化|
</sub></sup>


> Much previous work on subtyping first defines ground types, which are types not containing type variables, and then defines polymorphism by quantification over ground types [4, 2].

> 多くの以前の研究ではサブタイピングは最初にグランドタイプを定義し、それは制約のない型変数の型で、定量化された多相的グランド型を定義しました[4, 2]。

> サブタイプの多くの以前の研究は、最初の型変数を含まないタイプである地上型を定義し、その後、グランドの型の上に定量化することによって多型を定義して、[4]、[2]。

サブタイピングの多くの以前の研究は、最初に型変数を含まない方でグランド型を定義し、グランド型上に定量化された多相型を定義しました[4, 2]。

### 4.2

> <sup><sub>|
leads|先駆け|
surprisingly|驚くほど|
finicky|気難しいです|
</sub></sup>

> This leads to a surprisingly finicky subtyping relation between polymorphic types.

この先駆けは驚く程気難しいサブタイピングで多相的な型と関係しています。

> <sup><sub>|
Quantifying|定量|
admits|認めている|
proving|証明|
relationships|関係|
</sub></sup>

> Quantifying over ground types admits case analysis over types as a means of proving subtyping relationships between polymorphic types.

グランド型上の定量化は多相型の間のサブタイピング関係の証明できる意味の型上の解析できるケースを許可します。

> <sup><sub>|
Essentially|基本的に|
bakes|焼く|
closed-world|閉じた世界|
assumption|仮定|
</sub></sup>

> Essentially, defining polymorphic subtyping in terms of ground types bakes in a closed-world assumption.

基本的に、グランドタイプの項の多相的サブタイピングを定義する事は閉じた世界の仮定で焼きます。

### 4.3

> <sup><sub>|
Instead|代わりに|
reformulate|再公式化する|
axiomatisation|公理atisation|
</sub></sup>

> Instead, we reformulate subtyping by giving an algebraic axiomatisation of subtyping.

かわりに、われわれはサブタイピングを与えられたサブタイピングの代数的公理によって再公式化します。

> <sup><sub>|
whose|その|
counterintuitive|直観に反した|
relies|依存している|
nonexistence|非存在|
certain|一定|
thereby|それによって|
</sub></sup>

> Some counterintuitive subtyping relations whose truth relies on the nonexistence of certain types are thereby false in our system.

いくつかの直感に反した正しい依存している一定の型に非存在のサブタイピング関係はそれによって我々のシステム上では偽です。

> <sup><sub>|
definition|定義|
reasoning|推論|
precluded|排除|
</sub></sup>

> Our definition uses only open-world reasoning: case analysis on a type variable is precluded.

我々の定義は開かれた世界の推論でのみ使っています: 型変数上の解析出来るケースは排除されます。


#### 4.4

> <sup><sub>|
relating|関連|
finite|有限な|
automata|オートマトン|
</sub></sup>


> By relating this algebraic structure to the theory of regular languages, we are able to simplify the types inferred by our system using standard algorithms from the theory of finite automata.

この正規言語の定理からの代数的構造関連から、我々は有限オートマトンの理論から我々のシステムを使って標準的なアルゴリズムで推論された型を単純化する事が可能です。

# 5 Implementation

### 5.1

> <sup><sub>|
simplification|簡素化|
subsumption|包摂|
</sub></sup>


> We have implemented a simple functional language based on these ideas, which supports type inference, type simplification using automata, record types with structural subtyping and a polymorphic subsumption checker for verifying type annotations.

我々はこれらのアイディアをベースとした型推論をサポートし、簡素化された型オートマトンを使い、部分的構造型のレコードタイプと、型注釈の確認のための多相的包摂チェッカーを持った、シンプルな関数型言語を実装しました。

### 5.2

> We used our implementation to infer and simplify types for the functions in OCaml’s standard List module2.

> 我々は我々の関数の型を推論し簡潔化する我々の実装をOCamlの標準Listモジュールに使いました。

> google: 私たちは、OCamlの標準のListモジュールの関数の型を推論するために私たちの実装を使用していました。

我々は我々の関数の型を推論し簡潔化する我々の実装をOCamlの標準Listモジュールに使いました。


> <sup><sub>|
syntactically|構文的には|
as compact as|以下のようにコンパクト|
identical|同一の|
</sub></sup>

> The types inferred by MLsub were as compact as those inferred by OCaml, and in most cases were syntactically identical.

> MLsubの型推論はOCamlの推論のようにコンパクトで、しかも、多くは同一の構文的なケースです。

> google: MLsubによって推測のタイプはOCamlので推論したものとコンパクトにした、ほとんどの場合、構文的に同一でした。

MLsubによって推論された型はOCamlで推論した物をコンパクトにしたものと、ほとんどの場合、構文的に同一でした。

### 5.3

> <sup><sub>|
available|利用できます|
interactively|対話形式で|
author’s|著者の|
</sub></sup>

> The implementation is available from, and can be used interactively on the first author’s website:

> 実装は利用出来て、最初の著者のウェブサイト上で対話的に実行出来ます。

> google:実装は、から入手可能であり、第一著者のウェブサイト上で対話的に使用することができます。

実装は第一著者のウェブサイト上から入手可能で、対話的に実行出来ます。


http://www.cl.cam.ac.uk/˜sd601/mlsub

## References

[1] L. Damas and R. Milner. Principal type-schemes for functional programs. In Proceedings of the 9th ACM SIGPLANSIGACT Symposium on Principles of Programming Languages, POPL ’82, pages 207–212, New York, NY, USA, 1982. ACM.

[2] J. Niehren and T. Priesnitz. Entailment of non-structural subtype constraints. In P. Thiagarajan and R. Yap, editors, Advances in Computing Science — ASIAN’99, volume 1742 of Lecture Notes in Computer Science, pages 251–265. Springer Berlin Heidelberg, 1999.

[3] F. Pottier. Type inference in the presence of subtyping: from theory to practice. PhD thesis, Universite Paris 7, 1998. ´

[4] V. Trifonov and S. Smith. Subtyping constrained types. In R. Cousot and D. A. Schmidt, editors, Static Analysis, volume 1145 of Lecture Notes in Computer Science, pages 349–365. Springer Berlin Heidelberg, 1996.
