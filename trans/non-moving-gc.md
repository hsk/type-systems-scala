# An Efficient Non-Moving Garbage Collector for Functional Languages

以下の適当な翻訳です。

http://www.pllab.riec.tohoku.ac.jp/papers/icfp2011UenoOhoriOtomoAuthorVersion.pdf


Katsuhiro Ueno Atsushi Ohori Toshiaki Otomo ∗

Research Institute of Electrical Communication

Tohoku University

{katsu, ohori, o-toshi}@riec.tohoku.ac.jp

## Abstract

<sup><sub>
inter-operate 相互運用
efficient 効率的な
consist of 構成される
exponentially 指数関数的に
increasing 増加
Actual 現実の
reclaimed 再要求
maintains 維持
</sub></sup>

> <sup><sub>
Motivated by __developing a memory management system__ that allows __functional languages to seamlessly inter-operate with C__,
__we__ propose __an efficient non-moving garbage collection algorithm based on bitmap marking__ and report __its implementation and performance evaluation__.

__メモリ管理システムの開発__の目的は、__関数型言語とC言語のシームレスな相互運用__を可能にする事で、
__我々__は__効率的なbitmap markingwベースとしたnon-movingガーベジコレクションアルゴリズム__を提案し、そして__実装とパフォーマンスの評価__を報告します。

> <sup><sub>
In __our method__,
__the heap__ consists of __sub-heaps {Hi | c ≤ i ≤ B} of exponentially increasing allocation sizes (Hi for 2i bytes)__ and __a special sub-heap for exceptionally large objects__.

__我々の手法__では、__ヒープ__は__指数関数的に増加するアロケーションサイズ(2iバイトのHi)の複数のサブヒープ{Hi | c ≤ i ≤ B}__ と__指数関数的に大きなオブジェクト用の特殊なサブヒープ__で構成されます。

> <sup><sub>
__Actual space for each sub-heap__ is __dynamically allocated__ and reclaimed from __a pool of fixed size allocation segments__.

__各サブヒープの実際の空間__は__動的に確保され__、そして__アロケーションセグメントのサイズにあわせたプール__から要求されたものです。

> <sup><sub>
In __each allocation segment__,
__the algorithm__ maintains __a bitmap representing the set of live objects__.

__各々のアロケーションセグメント内__では、
__アルゴリズム__は__生存オブジェクトの集合のビットマップ表現__を維持します。

> <sup><sub>
__Allocation__ is done by __searching for the next free bit in the bitmap__.

__アロケーション__は__ビットマップ中の次のフリーなビットから検索することにより__行われます。

<sup><sub>
summarize 要約する
hierarchically 階層的に
hierarchy 階層
significant 重要な
beneficial 有益
</sub></sup>

> <sup><sub>
By adding __meta-level bitmaps__ that summarize __the contents of bitmaps hierarchically__ and maintaining __the current bit position in the bitmap hierarchy__,
__the next free bit__ can be found in __a small constant time for most cases__,
and in __log32(segmentSize) time in the worst case on a 32-bit architecture__.

__ビットマップ階層のコンテンツ__をサマライズし、そして__ビットマップ階層の現在ビット位置__を維持する事で __メタレベルビットマップ__を追加し、
__次のフリーなビット__の検索は__多くの場合小さな定数時間__で終わり、
__32bitアーキティクチャ上のワーストケースではlog32(セグメントサイズ)時間__で検索可能です。

> <sup><sub>
__The collection__ is done by __clearing the bitmaps and tracing live objects__.

__コレクション__は__ビットマップのクリアと生存オブジェクトのトレーシング__によって行われます。

> <sup><sub>
__The algorithm__ can be extended to __generational GC by maintaining multiple bitmaps for the same heap space__.

__アルゴリズム__ は__同じようなヒープスペースの管理する複数ビットマップ事により世代別GC__へ拡張出来ます。

> <sup><sub>
__The proposed method__ does not require __compaction__ and __objects__ are not moved __at all__. __This property__ is __significant for a functional language to inter-operate with C__,
and __it__ should also be __beneficial in supporting multiple native threads__.

__提案手法__ では __コンパクション__は必要とせず、__オブジェクト__は__全く__動かしません。__この性質__は __関数型言語とC言語の相互運用をするために重要__で、 __ネイティブなマルチスレッドのサポートをする上では__なくてはなりません。

> <sup><sub>
__The proposed method__ has been implemented in __a full-scale Standard ML compiler__.

__提案手法__は__フルスケールのStandard ML コンパイラ__で実装されています。

> <sup><sub>
__Our benchmark tests__ show that __our nonmoving collector__ performs as efficiently as __a generational copying collector designed for functional languages__.

__我々のベンチマークテスト__では__我々のnonmoving コレクタ__は__関数型言語用に設計された世代別コピーコレクタ__と同様の効率的なパフォーマンスであることを示しています。

## 1. Introduction

<sup><sub>
interoperability 相互運用性
partially 部分的に
achieved 達成
as well as 及び
therefore 故に
</sub></sup>

> <sup><sub>
__The general motivation of this research__ is __to develop a memory management system for a functional language__ that supports __seamless interoperability with the C language__.

__この研究の一般的モチベーション__は__C言語とのシームレスな相互運用__をサポートした__関数型言語用のメモリ管理システムの開発__です。

> <sup><sub>
__We__ have partially achieved __this goal through type-directed compilation for natural data representations in ML [25]__.

__我々__はこの目標を__MLでの自然なデータ表現の型の直接的なコンパイル[25]__ で部分的に達成しました。

> <sup><sub>
Under __this scheme__, __ML records and arrays as well as atomic types such as int and real__ have __the same representation as in C__ and are __therefore directly read or updated by a C program without any data representation conversion__.

__このスキーム__下では、__MLレコードと配列及びintやreal等のアトミックなタイプ__は__Cと同じ表現__をもち、そして__データ表現の変換無しに、Cプログラムから直接的に読み込め、更新__できます。

<sup><sub>
embodied 具体化
fragment 断片
distributed 分散型の
</sub></sup>

> <sup><sub>
__This__ has been embodied in __our SML# compiler [33]__.

これは、__SML# コンパイラ[33]__で具体化されています。

> <sup><sub>
In __this implementation__, __one__ can directly import __a C function__ and call __it with data structures such as records, mutable arrays, and function closures (for call-backs) created in SML#__.

__この実装__の中で、__Cの関数__は直接インポート出来て、__レコード、更新可能な配列、SML#で作成されたコールバック用の関数クロージャのようなデータ構造__を呼び出せます。

> <sup><sub>
__The following code__ is __a fragment of a demo program distributed with SML#__.

__以下のコード__は__SML#のデモプログラムのディストリビューション内の断片__です。

```
val glNormal3dv =
    dlsym (libgl, "glNormal3dv")
    : _import (real * real * real) -> unit
...
  map (fn (vec, ...) => (glNormal3dv vec; ...))
    [((1.0, 0.0, 0.0), ...), ...]
```

<sup><sub>
precision 精密
correctly 正しく
indicating 示します
whether かどうか
exactly 正確に
adopting 採用
should be able to achieve 達成することができるはずです
interoperability 相互運用性
</sub></sup>

> <sup><sub>
__This code__ dynamically links __glNormal3dv function in the OpenGL library as an ML function of type real * real * real -> unit__ and uses __it with other SML# code__.

__このコード__は__型 real * real * real -> unit のML関数であるOpenGLライブラリ内のglNormal3dv関数__を直接リンクし、 そして__他のSML#コードで__使います。

> <sup><sub>
__SML#__ compiles __(1.0, 0.0, 0.0)__ to __a pointer to a heap block containing 3 double precision floating point numbers__.

__SML#__は__(1.0, 0.0, 0.0)__を3つのdoubleにコンパイルします。

> <sup><sub>
Since __this record__ has __the same representation as a double array of C__, __glNormal3dv__ works __correctly__.

なぜなら__レコード__が__Cのdouble配列と同じ表現__持っているため、__glNormal3dv__は__正確に__働きます。

> <sup><sub>
In __addition to this natural data representation__, __each SML# heap object__ has __a header containing object layout information indicating__ whether __each field__ is __a pointer or not__.

__この自然なデータ表現の追加__により、__各SML#ヒープオブジェクト__は__各フィールド__が__ポインタかそうでないか__を__示すオブジェクトレイアウト情報を含んだヘッダ__を持っています。

> <sup><sub>
__This information__ is used __by the SML# garbage collector (GC) to exactly trace live objects__.

__この情報__は__正確に行きているオブジェクトをトレースするためにSML#ガーベジコレクター(GC)によって__使われています。

> <sup><sub>
By __adopting the strategy__ that __the SML# GC__ only traces and collects __SML# objects__ and leaves __management of C allocated objects to C code__, __we__ should be able to achieve __seamless interoperability between SML# and C__.

__SML# GC__が__SML#のオブジェクト__をトレースしコレクトし、そして__Cコード用にCがアロケートしたオブジェクトの管理__を残す戦略を採用する事で、我々は__シームレスなSML#とCの相互運用を__達成することができるはずです。

<sup><sub>
so far これまでの
as well as だけでなく
</sub></sup>


> <sup><sub>
__The solution so far__ is however __only partial__ in that __data structures__ that are passed to __foreign functions__ must be allocated in __a special non-moving area__.

__これまでのソリューション__はしかし__外部関数__は__特別なnon-movingエリア__内にアロケーションされる必要性があることから__データ構造__の中の部分的なもののみでした。

<sup><sub>
commonly 一般的に
accepted に受け入れられ
belief 信念
being であること
amount 量
would be なります
</sub></sup>

> <sup><sub>
__SML# compiler__,
__as well as most other functional language compilers__,
has used __copying garbage collection (GC) based on the commonly accepted belief being__ that,
__for functional programs requiring large amount of short-lived data__,
__the Cheney’s copying collector [8]__ would be __the most efficient for their minor collection__.

__SML#コンパイラ__、
__だけでなく多くの他の関数型言語コンパイラ__、
__一般的に受け入れられ信用のあるベースとなるコピーガーベジコレクション(GC)__を使っています、
__関数プログラムは短命な大量のデータ__を必要とし、
__Cheneyのコピーコレクタ[8]__は__それらのマイナーコレクション用のより効率的に__なります。
。


<sup><sub>
precise 正確な
prohibits 禁止する
inter-operating 相互動作
</sub></sup>


> <sup><sub>
However, __any (precise) copying GC__ requires that __the runtime system__ must be able __to locate and update all the pointers to each heap allocated data__.

しかしながら、多くの(正確な)コピーGCは__ランタイムシステム__が__ヒープアロケーションされたデータの全てのポインター再配置と更新__が可能である事が必要です。

> <sup><sub>
__This__ prohibits __functional programs from inter-operating with foreign functions or any programs__ that use __local memory space__ not accessible from __the garbage collector__.

__これ__は__外部関数か別のプログラムによる相互動作からの関数的プログラム__を禁止し、__ガーベジコレクタ__からアクセス出来ない__ローカルメモリ空間__を使います。

<sup><sub>
side-step サイドステップ
explicitly 明示的
</sub></sup>

> <sup><sub>
To __side-step this problem__, __the programmer__ must explicitly request __GC__ not to move __those data__ that are passed to __external code__.

__この問題のサイドステップ__から、__プログラマ__は__外部コード__へ渡されたそれらのデータは動かない正確な_GC_を要求します。

<sup><sub>
object pinning オブジェクトのピン止め
cumbersome 面倒な
not only A but also B AだけでなくBも
</sub></sup>

> <sup><sub>
__This “object pinning” approach__ is not only __cumbersome__ but also __dangerous__.

__"オブジェクトピン留め"アプローチ__は__面倒__なだけではなく、__危険__でもあります。

<sup><sub>
should be でなければなりません
painfully 苦痛なほど
familiar おなじみの
anyone 誰でも
interacts 相互作用
</sub></sup>



> <sup><sub>
__This problem__ should be __painfully familiar to anyone__ who has tried __to write a functional program__ that interacts with __a foreign library__ that requires __callbacks or locally stores object pointers passed from the caller__.

__関数プログラムを書くこと__を試みた __誰にでも苦痛な程おなじみの__ __この問題__ は __コールバックまたは呼び出し元から渡されたローカルストアのオブジェクトポインタ__を必要とする __外部ライブラリ__ との相互作用ができなければなりません。

<sup><sub>
rather むしろ
interoperability 相互運用性
provide 提供します
</sub></sup>

> <sup><sub>
For __a language with rather limited interoperability__,
__“object pinning”__ might be performed __automatically__,
but for __an MLstyle language__ that provides __seamless interoperability, automatic “object pinning”__ is __difficult, if not impossible__.

むしろ限定された相互運用性の言語のために、__オブジェクトピン留め__ は自動的に働くでしょう、しかし__MLスタイルの言語__では シームレスに相互運用性を提供する事、自動的なオブジェクトのピン留めは難しいですが、不可能ではありません。

<sup><sub>
suppose 仮定する
</sub></sup>

> <sup><sub>
To __see the problem__,
__suppose a C function__ is called with __an array and a call-back function__.

__問題を見る__ために、__仮定するC関数__は__配列とコールバック関数__によって呼ばれます。

<sup><sub>
mutate 変異させ
estimate 推定する
entire 全体の
even those でも、それら
</sub></sup>

> <sup><sub>
Since __both the C function and the call-back function__ can freely mutate __the array__,
__the runtime system__ can only safely estimate that __the set of reachable objects from the array passed to the C function__ to be __the set of all the live objects in the entire heap__,
including even _those_ that may be __created later by the call-back function__.

なぜなら__C関数とコールバック関数の両方__は__配列を__自由に変える事が出来、
__ランタイムシステム__は唯一安全に__C関数へ渡された配列から到達可能なオブジェクトの集合__ が __ヒープ全体の全てのいきたオブジェクト__となると推定出来て、__コールバック関数で後から作られた__ それらにも含まれている。

----

<sup><sub>
suitable 適切な
rely 頼る
heavily 重く
ideally 理想的
currently 現在
widely 広く
</sub></sup>


> <sup><sub>
To __solve this problem__, we would like __to develop a non-moving garbage collection algorithm suitable for functional languages__.

__この問題をとく__には、我々は__関数型言語のための適切なnon-movingガーベジコレクションアルゴリズムを開発__しなければならない。

> <sup><sub>
Since __functional programs__ rely __heavily on efficient allocation and collection__,
a __new non-moving GC algorithm__ should ideally be as efficient as __currently widely__ used __copying GC with generational extension [21, 35]__.

なぜならば、__関数プログラム__は__重く効率的なアロケーションとコレクション__に頼っており、 __あたらしいnon-moving GCアルゴリズム__は理想的には__現在広く使われているコピーGCの一般的な拡張 [21, 35]__ と同じくらい効率的であるべきです。

<sup><sub>
purpose 目的
feasibility 実現可能性
</sub></sup>


> <sup><sub>
__The purpose of this paper__ is to __develop such a garbage collection algorithm__,
to __implement it for a full-scale ML compiler__,
and to __evaluate its performance through extensive benchmarks to verify the feasibility of the algorithm__.

__この論文の目的__ は __そのようなガーベジコレクションアルゴリズムを開発する__事であり、
__そのフルスケールのMLコンパイラを実装する事__であり、
__そのアルゴリズムでの実現可能性の検証のための拡張のベンチマークのパフォーマンスを評価する事__です。

----

<sup><sub>
obvious 明白
candidate 候補
</sub></sup>

> <sup><sub>
__An obvious non-moving candidate__ is __mark and sweep GC[24]__ .

__明白なnon-movingの候補__は__マークアンドスイープGC [24]__ です。

<sup><sub>
tends 傾向
exhibit 展示
weaknesses 弱点
</sub></sup>

> <sup><sub>
Compared __with copying GC__, however, __simple mark and sweep GC__ tends to exhibit __the following general weaknesses__.

__コピーGC__と比べて、しかしながら、__シンプルなマーク＆スイープGC__は__以下の一般的な弱点__を示す傾向があります。

----

<sup><sub>
Fragmentation 断片化
various さまざまな
Due to に起因し、のために
becomes なります
reclamation 再利用
avoid 避ける
contrast コントラスト、対比
yields 利回り
boundary 境界
</sub></sup>

-   > <sup><sub>
    Fragmentation and slow allocation.

    断片化と遅いアロケーション。

    > <sup><sub>
    __Functional programs__ require __objects of various sizes with various life time__.

    __関数プログラミング__は__様々なライフタイムを持つ様々なサイズのオブジェクト__を必要とします。

    > <sup><sub>
    Due to this property, __the heap space__ becomes fragmented __very quickly__, resulting in __slow allocation and reclamation__.

    > <sup><sub>
    To __avoid this problem__, __practical variant of mark-and-sweep GC algorithms sometimes__ perform __costly compaction at sweep phase__.

    > <sup><sub>
    In __contrast__, __copying GC__ automatically performs __compaction__ and yields __a very fast allocator__, which has only __to check the heap boundary__ and __to advance the allocation pointer__.

<sup><sub>
proportional 比例します
Difficulty 難易度
straightforward 単純明快
certain 一定
machinery 機械
</sub></sup>

-   > <sup><sub>
    __High sweep cost__.
    __Mark and sweep GC__ requires __to sweep the heap__, which takes __time proportional to the size of the heap__.
    __Again this__ is __in contrast with copying GC__ whose __collection cost__ is __proportional to the amount of live data__.

-   > <sup><sub>
    __Difficulty in developing efficient generational GC__.
    While __it__ is __straightforward to combine mark and sweep GC with other GC to form a generational GC__,
    __developing generational mark and sweep GC__ requires __certain amount of additional machinery__, which would __result in slower GC time [10]__.

<sup><sub>
Perhaps おそらく
due to に起因する
seems to be considered 考慮されると思われます
suitable 適切な
produces 生成
varied 多様な
rapidly 急速に
</sub></sup>

> <sup><sub>
Perhaps __due to these problems__, __mark and sweep GC__ seems to be __considered not suitable for primary GC (e.g. minor GC in generational GC) of functional programs__,
which produces __large amount of short-lived data of varied size very rapidly__.

----

<sup><sub>
technical テクニカル
contribution 貢献
demonstrate 実証します
feasibility 実現可能性
survey サーベイ
series 一連
overcome 乗り越える
mentioned 言及
summary 要約
</sub></sup>

> <sup><sub>
__The main new technical contribution of this paper__ is to develop __a variant of mark and sweep GC__ that is __as efficient as Cheney’s copying GC and its generational extensions__,
and to demonstrate __its feasibility for functional languages thorough implementation and evaluation__.

> <sup><sub>
__Our basic strategy__ is __well known bitmap marking (see [37] and [17] for a survey.)__

> <sup><sub>
__We__ associate __a heap with bitmaps__ that represent __the set of live objects__.

> <sup><sub>
__This structure alone__ does not yield __an efficient allocation and collection__.

> <sup><sub>
__We__ have developed __a series of new data structures and algorithms__ that overcome __the weakness of mark and sweep GC mentioned above__.

> <sup><sub>
__The following__ is __a summary of the features of our GC algorithm__.

1.  > <sup><sub>
    __A fragmentation-avoided and compaction-free heap__.

    > <sup><sub>
    __Fragmentation__ occurs when __objects have varied sizes__; if __all the objects__ were of __the same size__, then __the heap__ could be __a fixed size array__ that can be __managed by a bitmap without incurring fragmentation__.

    > <sup><sub>
    __An extreme idea__ is to set up __a separate heap for each object size__.

    > <sup><sub>
    Of course __we__ cannot prepare __sufficiently large separate heaps for all possible object sizes__,
    so __we__ prepare __a series of sub-heaps {Hi | c ≤ i} of exponentially increasing allocation block sizes__,
    i.e. __each Hi consists of allocation blocks of 2i bytes__.

    > <sup><sub>
    Actual __allocation space of Hi__ is __dynamically maintained as a list of fixed-size segments__.
    
    > <sup><sub>
    __Each segment__ contains __an array of 2i-byte blocks__.
    
    > <sup><sub>
    __2c__ is __the minimum allocation block size in bytes__.

    > <sup><sub>
    __In SML#__, __the minimum size of non-empty objects__ is __8(2 3) byte__, so __we__ fix __c to be 3 in this paper__.

    ----

    > <sup><sub>
    __These structures__ eliminate __the fragmentation problem associated with mark and sweep collection__.

    > <sup><sub>
    Since __a segment__ is __a fixed size array__,
    __it__ is __efficiently maintained by a bitmap__ where __each bit corresponds to one block__.

    > <sup><sub>
    Moreover, __size of each Hi__ is __dynamically adjusted through allocation and reclamation of segments__.

    > <sup><sub>
    In __our scheme__, __an allocation block in Hi in general__ contains __an object smaller than 2i bytes__,
    and __some amount of memory__ is __unused__.

    > <sup><sub>
    __Our observation__ is that, __we__ can avoid costly compaction at __the expense of locally wasting space in each allocation block__,
    and that __this cost__ is __quite acceptable in practice__.

    > <sup><sub>
    __Our evaluation__ shows that __the space usage__ is better than __that of copying GC in most cases__.

    __我々の評価器__は__スペース効率の点で__ __多くの場合コピーGC__より上回っていることを示します。

    ----

    > <sup><sub>
    Since __we__ cannot prepare __Hi for unbounded i__, __we__ set __a bound B of i__ and allocate __exceptionally large objects of size more than 2B to a special sub-heap M__.

    > <sup><sub>
    __B__ can be __as large as the size of one segment__.

    > <sup><sub>
    According to __our experimentation__, __B = 12 (i.e. at most 4096 bytes)__ appears __to be sufficient for functional programs (e.g. covers most of allocation requests.)__

    > <sup><sub>
    Hence, in __our system__, __the heap consists of 10 sub-heaps__, H3, . . . H12.

    > <sup><sub>
    Since __the special sub-heap M__ occupies __a very small portion of the entire heap__ and can be managed by __any non-copying GC method__, __we__ do not consider __this further__.


2.  > <sup><sub>
    __Efficient allocation__.

    > <sup><sub>
    __A bitmap__ can be used not only __for marking__ but also __for allocation by searching for a free bit in a bitmap__.

    > <sup><sub>
    __Simple search for a free bit in a bitmap__ takes,
    __in the worst case__,
    __the time proportional to the number of busy bits__.

    > <sup><sub>
    Perhaps __due to this problem__, __most of bitmap based GC algorithms__ use __some form of free list for allocation__.

    > <sup><sub>
    __We__ solve __this problem by adding meta-level bitmaps__ that __summarize the contents of bitmaps__.

    > <sup><sub>
    __The set of all bitmaps form a hierarchically__ organized __tree__.

    > <sup><sub>
    __The sequence of bits at the leaf level in the tree__ is __the ordinary bitmap representing liveness of the set of allocation blocks__,
    and __the sequence of bits at an intermediate level__ summarizes __the bitmap one level below__.

    > <sup><sub>
    On __this bitmap tree__,
    __we__ maintain __a data structure representing both the next bit position to be examined and the corresponding block address__.

    > <sup><sub>
    __This structure__ corresponds __to the allocation pointer of copying GC__,
    so __we__ call __it an allocation pointer__.

    > <sup><sub>
    __The whole organization__ is __depicted in Figure 2__ whose __details__ will be __explained in Section 2__.

    > <sup><sub>
    By __constructing a series of optimized searching algorithms on this data structure__,
    __the next free bit__ can be found __in a small constant time for most cases__,
    and __in log32(segmentSize) time in the worst case__.

3.  > <sup><sub>
    __Small GC cost__.

    > <sup><sub>
    __Sweeping__ is done __by clearing bitmaps__.

    > <sup><sub>
    __The total collection cost of a bitmap making GC__ is __the sum of the costs for clearing bitmaps__,
    __tracing live objects__,
    and __setting the bits in the bitmaps__.

    > <sup><sub>
    Among __them__,
    __the bitmap clearing__ requires __N/32 steps__ where __N__ is __the total number of allocation blocks__.

    > <sup><sub>
    Our __extensive evaluation__ shows that __bitmap clear time__ is __negligible (about 1% of the GC time)__,
    and therefore, in practice,
    __the total cost of our GC__ is __dominated by the tracing and marking cost__.

    > <sup><sub>
    So __in practice our GC algorithm__ behaves __similarly to that of copying GC__.

    > <sup><sub>
    In __most of the benchmark tests we performed__, __the total GC cost__ was __smaller than those of simple copying collector and generational copying collector__.

4.  > <sup><sub>
    __Non-moving generational GC__.
    
    > <sup><sub>
    __By adapting the idea of partial collection__ proposed __by Demers et.al. [10]__,
    __our bitmap marking GC__ scales up to __generational GC without moving objects__.

    > <sup><sub>
    __This__ is based on __our observation__ that __a bitmap__ represents __a subset of the set of objects in a heap__.

    > <sup><sub>
    __Generational GC__ can be realized by __maintaining a separate bitmap for each generation__.

    > <sup><sub>
    __The partial__ collection __with “sticky bit” technique presented in [10] coincides with a special case__ where __tenuring threshold__ is __1__ and __the number of generations__ is __2__.

    > <sup><sub>
    __Pointers to younger generations from older generations__ are tracked using __a write barrier and a remembered set__.

    > <sup><sub>
    In __the above special case__, __the remembered set__ can be allocated in __the collector’s trace stack due to the property__ that __the remembered set__ can be __flushed after minor collection__.

    > <sup><sub>
    __The resulting implementation of the special case__ does not require __any additional memory space other than those used in the non-generational version__.

----

> <sup><sub>
__We__ have implemented __the data structures and algorithms in a Standard ML compiler__,
and have done __extensive benchmark tests__.

> <sup><sub>
__We__ have evaluated and compared __them against a simple Cheney’s copying GC__,
and __a generational copying GC for functional languages described in [29]__,
and have obtained __the following results__:

> <sup><sub>
(i) __segment-based dynamic sub-heap size adjustment__ automatically achieves __optimal hand-tuned sub-heap size assignment__,
(ii) __the bitmap marking GC__ is __as efficient as Cheney’s copying GC__,
and (iii) __the generational extension__ outperforms __the non-generational bitmap GC__,
and shows __comparable or better performance results compared to generational copying GC__.

> <sup><sub>
__These results__ demonstrate that __our development__ have achieved __the goal of developing a nonmoving GC method for functional language__.

> <sup><sub>
__The proposed method__ has __additional advantage of supporting multiple native threads without much additional machinery__.

> <sup><sub>
__Our segment-based heap organization__ allows __each concurrent thread to allocate objects in a shared global heap without any locking__. 

> <sup><sub>
__The detailed implementation and evaluation for multithread extension__ is __out of the scope of the present paper__, but __our preliminary implementation of multithread extension__ shows __promising result__.

> <sup><sub>
__All the implementation__ is done __in our SML# compiler__, which compiles __the full set of Standard ML language (with the (mostly) seamless interoperability with C) into x86 native code__.

> <sup><sub>
__SML# version 0.40 or later__ includes __the GC algorithms reported in this paper__,
and __they__ are __available and enabled by default in x86 native compilation mode__.


> <sup><sub>
__The rest of the paper__ is __organized as follows__.

論文の残りは以下のような編成です。

> <sup><sub>
__Section 2__ presents __the bitmap marking GC algorithm__.
__Section 3__ extends __the GC method to generational GC__.
__Section 4__ reports __implementation and performance evaluation__.
__Section 5__ compares __the contribution with related works__.
__Section 6__ concludes __the paper__.

2章では、bitmap マーキングGCアルゴリズムを提案します。
3章では、世代別GCのGCの手法を拡張します。
4章では、実装とパフォーマンスの評価を報告します。
5章では、他の研究と貢献を比較します。
6章では、この論文のまとめます。

## 2. The bitmap marking GC

This section describes the details of the data structures and algorithms
for the bitmap marking GC. This description involves lowlevel
bit manipulation. To achieve efficient allocation with bitmaps,
we found it essential to design data structures and algorithms in
detail. Accurately reporting them requires us to present them in details,
sometimes referring to bit-level manipulation. For this reason,
we use C like syntax in describing the algorithms.

### 2.1 The GC-mutator interface

### 2.2 The structure of the heap space and allocation strategy

In what follows, we assume that one machine word is 32-bit long.
This is not a restriction; any other word size can equally be used.
We use a given allocation space as a pool of fixed size allocation
areas, called segments, and set up the entire heap as follows.
heap = (M, S,(H3, . . . , H12))
M is a special sub-heap for large objects explained earlier. S is a
free segment pool, i.e. set of unused segments. Each Hi is a subheap
for 2
i
byte allocation blocks. Each sub-heap has the following
structure.
Hi = (SegList i
, Pi)
SegList i
is a list of segments currently belonging to Hi. Pi is an
allocation pointer for sub-heap Hi whose structure is defined in
subsection 2.4.
A segment Si initialized for Hi has the following structure.
Si = (Count i, Blks i, BitMapi
, Tworki)
where Count i is the number of already allocated blocks in this
segment, Blks i is an array of 2
i
byte allocation blocks, BitMapi
is
a bitmap tree, and Tworki is the working area used for tracing live
objects. The number of allocation blocks in a segment is derived
from the segment size, which is statically fixed. We write Sizei
for the number of allocation blocks in Si. We assume that every
segment is aligned to power-of-2 boundary for fast bit-level address
computation.
We write Blks i(k) for the k-th block in Blks i. Let Li =
dlog32(Sizei)e. This determines the height of the bitmap tree in
a segment Si. The bitmap tree has the following structure
BitMapi = (BM 0
i
, . . . , BM Li−1
i
)
where BM j
i
is the j-th level bitmap which is a sequence of bits
organized as an array of 32 bit words. We write BM j
i
(k) to denote
the k-th bit and BM j
i
[k] the k-th word in the j-th level bitmap. The
least significant bit in BM j
i
[k] is the bit BM j
i
(32 × k + 0) and its
most significant bit is BM j
i
(32 × k + 31). The leaf-level bitmap

BM 0
i
represents the liveness of Blks i, namely,
BM 0
i (k) = {
1 Blks i(k) is live,
0 Blks i(k) is free.
As we shall explain below, meanings of 1 and 0 are chosen in such
a way that free bit search can be implemented efficiently. The j+1-
th level bitmap BM j+1
i
represents whether each word in BM j
i
has
free entry or not, namely,
BM j+1
i
(k) = {
1 all bits in BM j
i
[k] are 1,
0 otherwise.
So for example if all the bits of the top-level bitmap BM Li−1
i
is 1
then all the blocks in Blks i are live, and there is no space left.
### 2.3 The allocation strategy

With the above structure, SegListi in Hi forms a single allocation area managed by one (virtual) hierarchically organized tree of bitmaps.

The list itself is regarded as the root bitmap of the entire bitmap tree where each bit indicates whether each segment is full or not, and each segment in the list is regarded as an immediate sub-tree of the root bitmap.

This structure guarantees that the next free block in Hi can be found in log32(Sizei) time in the worst case.

In addition, we need to make average allocation as efficient as possible so that it can be comparable to “bump allocation” in Cheney GC.

To this end, we observe that in most cases the block array Blks i is very sparsely used after GC.

So we adopt the following strategy.

1. We sequentially allocate the next free block in Hi.

2. To make the typical case of allocation fast, we maintain a position information of the next candidate of allocation block.

If this block is free then the allocator simply returns this next block and advances the position information.

3. If the next block is live, then the allocator searches for the next free bit using the bitmap tree. To perform this search efficiently, we maintain the next bit position information for higher-level bitmaps.

The allocation pointer Pi in Hi introduced in the previous subsection is for this purpose, whose structure is given below.

    Pi = (Si, BitPtrs i, BlkPtr i)
    BitPtrs i = (BitPtr 0i, . . . , BitPtr Li−1i)

Si is a pointer to the active segment in Hi, i.e. the segment in which blocks are being allocated.

BitPtrs i are bit pointers indicating the next bit positions in Si to be examined.

BitPtr 0i indicates the next bit position to be tested in the leaf-level bitmap BM 0i.

For each `0 ≤ j ≤ Li − 2, BitPtr j+1i` points to the parent bit representing the bitmap word that includes the bit pointed by BitPtr ji.

BlkPtr i is a block pointer indicating the block address corresponding to the bit pointed by BitPtr 0i.

Using these pointers, allocation is done as fast as “bump allocation” when the next block is free.

_Figure 1 and 2_ shows _the structures of the set of sub-heaps and a segment, respectively_.
図1と図2は、それぞれサブヒープとセグメントの集合の構造を示します。

![図1](https://raw.githubusercontent.com/hsk/type-systems-scala/master/trans/non-moving-gc-fig1.png)
図1

![図2](https://raw.githubusercontent.com/hsk/type-systems-scala/master/trans/non-moving-gc-fig2.png)
図2

![図1](non-moving-gc-fig1.png)
図1

![図2](non-moving-gc-fig2.png)
図2

### 2.4 The allocation algorithm

### 2.5 The bitmap marking GC algorithm


## 3. Generational extension

In order for our non-moving bitmap marking GC to become a
practical and truly better alternative to copying GC, we would like
to extend it to generational GC. It is of course straightforward to
combine our bitmap marking GC with any other GC to form a
generational GC by moving objects between two heaps managed
by two separate GC, but the resulting system loses the non-moving
property. Our goal is to develop a new method that achieves the
desired effect of generational GC without actually moving objects.
This section presents one such general method.

### 3.1 General strategy

### 3.2 The GC algorithm

## 4. Implementation and Evaluation

We have implemented the bitmap marking GC algorithm presented
in Section 2 and its generational extension presented in Section 3.
For comparison purpose, we have also implemented a plain Cheney
copying collector, and a generational copying GC algorithm for
Standard ML described in [29]. In this section, we outline our
implementation and show the detailed evaluation results.

### 4.1 Implementation

### 4.2 Performance Evaluation

## 5. Related works

## 6. Conclusions and further development
