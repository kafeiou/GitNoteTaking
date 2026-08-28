# Git Note Taking (Git テキストノート)

[繁體中文](README.md) | [English](README_en.md) | [日本語](README_ja.md) | [简体中文](README_zh-CN.md)

> **ソフトウェアエンジニア専用・一般の方はお控えください**

## 💡 特徴
1. **Git バージョン管理**：変更履歴とコミットメッセージを完全に保持。
2. **クラウド GitHub 同期**：無料のクラウドバックアップと複数端末間での同期。
3. **完全オフライン対応**：ネット環境がなくても快適にノートの閲覧・編集が可能。
4. **全文検索**：過去のノートやファイル内容を高速に検索。
5. **ローカル履歴パージ**：ワンタップでリポジトリを単一バージョン（`depth = 1`）にスリム化し、端末容量を節約。

## 🎯 設計理念
GitHub クラウドサービスを通じて日々のドキュメントを同期。オフラインで閲覧・編集し、都合の良いタイミングで GitHub へ Push して同期します。

**Git の強み**：編集ごとに理由（Commit Message）を記録できるため、後からの振り返りや履歴追跡が容易です。

## 📖 使い方

### 方法 1：GitHub ノートを作成（推奨）
1. アプリを開き、右上のメニューから最下部の **「GitHub ノートを作成」** をタップします。
2. **【Token を生成】** をタップすると、必要な権限（`repo` および `read:user`）が事前選択された状態で GitHub ページが開きます。
3. 「Expiration」を **No expiration（無期限）** に設定し、最下部で **Generate token** をタップしてコピーします。
4. アプリに戻ると、**クリップボードから Token が自動入力** されます。【接続する】をタップします。
5. アカウント内の `note` で始まるリポジトリ（例: `note-work`, `NoteTaking`）が一覧表示されますので、選択するだけで高速にクローンされ、利用を開始できます！

### 方法 2：カスタムリモート Git / ローカルノート
1. **リモート Git**：右上のメニューで「リモートノートを取得」を選択し、Git URL・ユーザー名・Token を入力します。
2. **ローカルノート**：右上のメニューで「ローカルノートを作成」を選択し、完全オフラインの Git リポジトリを作成します。

## 📦 Google Play リリース＆配布管理 (Release & Distribution)

- **`distribution/whatsnew/`**：Google Play ストア向けの **現行リリースノート** を各言語ごとに格納（Google Play の規約に基づき 500 文字以内に制限）：
  - `whatsnew-zh-TW`（繁体字中国語）
  - `whatsnew-zh-CN`（簡体字中国語）
  - `whatsnew-ja-JP`（日本語）
  - `whatsnew-en-US`（英語 / デフォルト）
- **`CHANGELOG.md`**：製品の全バージョン履歴を記録。

### 📌 リリース用 Prompt テンプレート (Release Prompt Template)
新バージョンをリリースする際は、以下の Prompt を AI に送信してください：
```text
新バージョン [バージョン番号、例: 4.002] のリリースをお願いします：
1. app/build.gradle の versionName と versionCode を更新。
2. CHANGELOG.md の最上部に新バージョンの更新履歴を追加。
3. distribution/whatsnew/ ディレクトリ内の 5 言語のリリースノートを上書き更新（各言語 500 文字以内）。
4. ./gradlew assembleDebug を実行してビルドと検証を行う。
```

## 🌐 オープンソース情報
- GitHub リポジトリ：https://github.com/WilliamFromTW/GitNoteTaking

## 📚 サードパーティライブラリ＆アセットライセンス
- [Eclipse JGit](https://www.eclipse.org/jgit) (version 7.4.0)
- Git Logo by Jason Long is licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
- 動作環境：Android 13 (API 33) 以降

## 🤖 開発ツールのクレジット
- 本プロジェクトは **Gemini CLI 1.1.22 版** および **OpenSpec 1.11.0** を活用して開発されました。
