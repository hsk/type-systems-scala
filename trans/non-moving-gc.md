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

## 2. The bitmap marking GC

<sup><sub>
so far これまでの
as well as だけでなく
</sub></sup>

> <sup><sub>
__The solution so far__ is however __only partial__ in that __data structures__ that are passed to __foreign functions__ must be allocated in __a special non-moving area__.

__これまでのソリューション__はしかし__外部関数__は__特別なnon-movingエリア__内にアロケーションされる必用があることから__データ構造__の中の部分的なもののみでした。

> <sup><sub>
__SML# compiler__, __as well as most other functional language compilers__, has used __copying garbage collection (GC) based on the commonly accepted belief being__ that, __for functional programs requiring large amount of short-lived data__, __the Cheney’s copying collector [8]__ would be __the most efficient for their minor collection__.

__SML#コンパイラ__、__だけでなく多くの他の関数型言語コンパイラ__、は持っています。

> <sup><sub>
However, __any (precise) copying GC__ requires that __the runtime system__ must be able __to locate and update all the pointers to each heap allocated data__.

しかしながら、多くの(正確な)コピーGCは__ランタイムシステム__が__ヒープアロケーションされたデータの全てのポインター再配置と更新__が可能である事が必要です。

> <sup><sub>
__This prohibits__ functional programs from __inter-operating with foreign functions or any programs__ that use __local memory space__ not accessible from __the garbage collector__.

> <sup><sub>
To __side-step this problem__, __the programmer__ must explicitly request __GC__ not to move __those data__ that are passed to __external code__.

> <sup><sub>
__This “object pinning” approach__ is not only __cumbersome__ but also __dangerous__.

> <sup><sub>
__This problem__ should be __painfully familiar to anyone__ who has tried __to write a functional program__ that interacts with __a foreign library__ that requires __callbacks or locally stores object pointers passed from the caller__.

> <sup><sub>
For __a language with rather limited interoperability__,
__“object pinning”__ might be performed __automatically__,
but for __an MLstyle language__ that provides __seamless interoperability, automatic “object pinning”__ is __difficult, if not impossible__.

> <sup><sub>
To __see the problem__,
__suppose a C function__ is called with __an array and a call-back function__.

> <sup><sub>
Since __both the C function and the call-back function__ can freely mutate __the array__,
__the runtime system__ can only safely estimate that __the set of reachable objects from the array__ passed to __the C function__ to be __the set of all the live objects in the entire heap__, including even those that may be created later by __the call-back function__.

### 2.1 The GC-mutator interface

### 2.2 The structure of the heap space and allocation strategy

### 2.3 The allocation strategy

### 2.4 The allocation algorithm

### 2.5 The bitmap marking GC algorithm

## 3. Generational extension

### 3.1 General strategy

### 3.2 The GC algorithm

## 4. Implementation and Evaluation

### 4.1 Implementation

### 4.2 Performance Evaluation

## 5. Related works

## 6. Conclusions and further development
