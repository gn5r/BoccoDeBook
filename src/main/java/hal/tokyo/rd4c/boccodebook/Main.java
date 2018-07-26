/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import hal.tokyo.rd4c.bocco4j.BoccoAPI;
import hal.tokyo.rd4c.nfc.NFCReader;
import hal.tokyo.rd4c.nfc.RaspRC522;
import hal.tokyo.rd4c.speech2text.Speaker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author gn5r
 */
public class Main {

    private static final int ERROR = -1;

    /* mode:CardSet, CardScan, RecVoice */
    private static String mode;
    private static String sendText;
    private static NFCReader nfcReader;

    /* 各音のファイルパス */
    private static final String setupSoundPath = "sound/setup.wav";
    private static final String buzzer = "sound/buzzer.wav";
    private static final String load = "sound/load.wav";
    private static final String OK = "sound/OK.wav";

    /* 物語格納用のパス */
    private static final String txtFilePath = "text/story.txt";
    private static String saveStory = "";

    private static String GOOGLE_API_KEY;

    /* BGM番号 */
    private static int BGMNum = 0;
    /* BGM再生用 */
    private static BGMPlayer BPlayer;

    /* ステージ回数0-3 */
    private static int stage = 0;
    /* 終わりflag */
    private static boolean endFlag = false;

    /*    1フレーズが完成したかどうか    */
    private static boolean phraseEnd;

    /*    Boccoに送信する際の文字列数    */
    private static final int CUT_LENGTH = 150;

    /* BoocoAPI(String APIKey, String Email, String Password) */
    private static BoccoAPI boccoAPI;
    private static TextMessage textMessage;

    /* 変換後文字列を格納するためのファイル名を格納 */
    public static String recFileName;

    /* GPIO */
    private static GpioPinDigitalInput startSE, event1SE, event2SE, endingSE, step, play, rec;
    private static GpioPinDigitalOutput startSELED, event1SELED, event2SELED, endingSELED;
    private static GpioController gpio;

    /* メイン */
    public static void main(String[] args) throws Exception {

        /* 初期化 */
        init(args);

        while (!endFlag) {
            /* mode = CardSet & stepボタンのリスナー設定 */
            cardSet(stage);

            /* 終わりカードがセットされていない間繰り返す */
            while (!phraseEnd) {
                Thread.sleep(1000);
            }
        }
        /* BOCCOに今までの文字列を送信 & 終了*/
        sendBoccoText(textMessage.readText(TextMessage.END_STORY));
        sendStory();
    }

    /* 初期化 */
    private static void init(String[] args) throws Exception {

        /* 引数の数が正しくない場合終了 */
        if (args.length != 4) {
            return;
        }

        /* args[0]:BOCCOAPI args[1]:Email args[2]:PassWord args[3]:GOOGLE_API_KEY */
        boccoAPI = new BoccoAPI(args[0], args[1], args[2]);
        GOOGLE_API_KEY = args[3];
        textMessage = new TextMessage();

        /* 起動音を鳴らす */
        soundPlay(setupSoundPath);

        File file = new File(txtFilePath);
        if (file.exists()) {
            /* ファイルが存在している場合は削除 */
            file.delete();
        }
        /* ファイルを作成 */
        createFile(txtFilePath);

        gpio = GpioFactory.getInstance();

        /*    各SEボタンとLED    */
        startSE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP);
        startSE.setShutdownOptions(true);

        startSELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "startSE", PinState.LOW);
        startSELED.setShutdownOptions(true, PinState.LOW);

        event1SE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_15, PinPullResistance.PULL_UP);
        event1SE.setShutdownOptions(true);

        event1SELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_16, "event1SE", PinState.LOW);
        event1SELED.setShutdownOptions(true, PinState.LOW);

        event2SE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_26, PinPullResistance.PULL_UP);
        event2SE.setShutdownOptions(true);

        event2SELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27, "Sevent2SE", PinState.LOW);
        event2SELED.setShutdownOptions(true, PinState.LOW);

        endingSE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_28, PinPullResistance.PULL_UP);
        endingSE.setShutdownOptions(true);

        endingSELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, "endingSE", PinState.LOW);
        endingSELED.setShutdownOptions(true, PinState.LOW);

        rec = gpio.provisionDigitalInputPin(RaspiPin.GPIO_21, PinPullResistance.PULL_UP);
        rec.setShutdownOptions(true);

        play = gpio.provisionDigitalInputPin(RaspiPin.GPIO_22, PinPullResistance.PULL_UP);
        play.setShutdownOptions(true);

        step = gpio.provisionDigitalInputPin(RaspiPin.GPIO_23, PinPullResistance.PULL_UP);
        step.setShutdownOptions(true);

        /* RC522_Initを複数呼び出すと危険なため */
        RaspRC522 rsp = new RaspRC522();
        rsp.RC522_Init();

        /* BOCCOの接続が確立できた場合 */
        if (boccoAPI.createSessions() == true) {
            boccoAPI.getFirstRooID();
            sendBoccoText(textMessage.readText(TextMessage.SESSION_OK));
        }
    }

    /* どのカードをセットするかBOCCOに喋ってもらう */
    public static void cardSet(int stage) throws Exception {

        phraseEnd = false;

        switch (stage) {
            /* 一枚目 */
            case 0:
                sendText = textMessage.readText(TextMessage.FIRST_SET);
                break;
            /* 二枚目,三枚目 */
            case 1:
            case 2:
                sendText = textMessage.readText(TextMessage.NOT_FIRST);
                break;
            /* 四枚目 */
            case 3:
                sendText = textMessage.readText(TextMessage.LAST_SET);
                break;
            /* 例外処理, 終了 */
            default:
                System.out.println("想定外の数値が入力、終了します。");
                return;
        }
//        System.out.println(stage + 1 + "枚目\n");
        /* ステップ押下の誘導メッセージ */
        sendText += textMessage.readText(TextMessage.CARD_STEP);
        sendBoccoText(sendText);
        /* カードセットが終了 */
        mode = "cardSetEnd";

        /* stepボタンのリスナーをセット => modeごとに動作を変更 */
        step.addListener(new StepButtonListener(stage, mode));
    }

    /* NFCリーダーでデータを読み込む.   false:エラー   0 - 14:正常 */
    private static int cardScan() throws Exception {
        nfcReader = new NFCReader();
        int BGMNum = ERROR;
        int cnt = 0;

        /* 正常な値が読み取れるまで or 5回読み取れなかった場合 */
        while (cnt < 5) {
            /* 何かしら値が取れた場合は強制的に抜け出す */
            if (BGMNum != ERROR) {
                return BGMNum;
            }
            /* カード情報の取得 */
            BGMNum = nfcReader.readBGMNum();
            Thread.sleep(500);
            cnt++;
            /* 読み込み中の音 */
            soundPlay(load);
        }
        return BGMNum;
    }

    /* カード番号取得後、正しいカードか判断 */
    public boolean cardJudge(int stage) throws Exception {

        /* 異常値：false, 正常値：true */
        boolean flag = false;
        BGMNum = cardScan();
        System.err.println("BGM番号：" + BGMNum);
        System.err.println("ステージ：" + stage);
        /* カード番号取得できなかったらBOCCOで教える & cardjudgeを抜け出す */
        if (BGMNum == ERROR) {
            sendBoccoText(textMessage.readText(TextMessage.NOT_SCAN));
            return flag;
        } else {
            /* 読み込み正常時の音 */
            soundPlay(OK);
        }

        /* ステージ数によって判断を変える */
        switch (stage) {
            /* 一枚目 */
            case 0:
                if (0 <= BGMNum && BGMNum < 3) {
                    flag = true;
                    /* SEボタンセット */
                    startSE.addListener(new SEButtonListener(startSELED, BGMNum));
                }
                break;
            /* 二枚目,三枚目 */
            case 1:
            case 2:
                if (3 <= BGMNum && BGMNum < 12) {
                    flag = true;
                    /* stageが1ならevent1ボタンを、2ならevent2のボタンをセットする */
                    if (stage == 1) {
                        /* SEボタンセット */
                        event1SE.addListener(new SEButtonListener(event1SELED, BGMNum));
                    } else if (stage == 2) {
                        /* SEボタンセット */
                        event2SE.addListener(new SEButtonListener(event2SELED, BGMNum));
                    }
                }
            /* 四枚目 */
            case 3:
                if (12 <= BGMNum && BGMNum < 15) {
                    flag = true;
                    /* SEボタンセット */
                    endingSE.addListener(new SEButtonListener(endingSELED, BGMNum));
                }
                break;
            /* 例外処理 */
            default:
                System.out.println("想定外の数値が入力されました。");
                break;
        }

        if (flag == true) {
            /* stepボタンのリスナー開放 */
            removeStepListener();
        } else {
            /* 違うカードが呼ばれたとき */
            sendBoccoText(textMessage.readText(TextMessage.NOT_CARD));
        }
        return flag;
    }

    /* ゲーム実行時呼ばれる */
    public void gamePlay() throws Exception {
        /* 読み込み完了メッセージ */
        sendBoccoText(textMessage.readText(TextMessage.READ_OK));
        /* BGMセット & BGM Start */
        BPlayer = new BGMPlayer(BGMNum);
        BPlayer.start();
        /* 録音ボタンのリスナーを設定 */
        addRecListener();
    }

    /* BGMの終了(録音終了のstep押下時) */
    public void BGMStop() {
        BPlayer.stopBGM();
    }

    /* stepとplayのリスナーを設定する */
    public void setListener() {
        mode = "recVoiceEnd";
        addStepListener();
        addPlayListener();
    }

    /* Listener開放 */
    public void removeStepListener() {
        step.removeAllListeners();
    }

    public void removePlayListener() {
        play.removeAllListeners();
    }

    public void removeRecListener() {
        rec.removeAllListeners();
    }

    /* Listener追加  */
    public void addStepListener() {
        step.addListener(new StepButtonListener(stage, mode));
    }

    public void addPlayListener() {
        try {
            BGMStop();
            BPlayer = new BGMPlayer(BGMNum);
            play.addListener(new PlayButtonListener());
            BPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRecListener() {
        rec.addListener(new RecordingButtonListener(GOOGLE_API_KEY));
    }

    /* 変換後文字列を格納するためのtxtファイル */
    private static void createFile(String filePath) throws IOException {
        File recorFile = new File(filePath);
        try {
            if (recorFile.createNewFile()) {
                System.out.println("ファイルの作成に成功しました。");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 音声->文字列化したものをファイルに書き込み */
    public void writeFile(String data) throws IOException {
        /* story.txt */
        File file = new File(txtFilePath);
        try {
            /* 書き込みが可能ならば */
            if (checkBeforeWritefile(file)) {
                /* 上書きなしで追加 */
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                /* 文字列の書き込み & 改行して保存 */
                pw.println(data);
                System.out.println("書き込み完了");
                /* ファイルを閉じる */
                pw.close();
            } else {
                System.out.println("ファイルに書き込めません");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 再生ボタンが押されたときの挙動 */
    public void recentlySend() throws Exception {
        /* 直近のデータを送信 */
        sendBoccoText(saveStory);
    }

    /* 書き込み可能か判断する. */
    private static boolean checkBeforeWritefile(File file) {
        if (file.exists()) {
            if (file.isFile() && file.canWrite()) {
                return true;
            }
        }
        return false;
    }

    /* 終わり判定＆カードセットへ行くかBOCCOに文字列を送信するか */
    public void endJudge() {
        System.out.println("endJudge()");
        if (11 < BGMNum && BGMNum <= 14) {
            endFlag = true;
        }
        /* 次のステージ（カード）へ移行 */
        phraseEnd = true;
        stage++;
    }

    /* BOCCOに今までの物語を送信する txt名:txtFilePath */
    private static void sendStory() throws Exception {
        String story = "";
        File file = new File(txtFilePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        int cnt = 0;

        try {
            System.out.println("物語送信開始");
            story = br.readLine();
            /* 全ての列を網羅 */
            while (story != null) {
                /* BOCCOに送れる範囲ならそのまま送信 */
                if (story.length() < CUT_LENGTH) {
                    sendBoccoText(story);
                    Thread.sleep(5000);
                } else {
                    /* CUT_LENGTHより長いならCUT_LENGTHずつ分けてBOCCOへ送信 */
                    for (cnt = 0; cnt + CUT_LENGTH < story.length(); cnt += CUT_LENGTH) {
                        sendBoccoText(story.substring(cnt, cnt + CUT_LENGTH));
                        Thread.sleep(5000);
                    }
                    /* 最後の行 */
                    sendBoccoText(story.substring(cnt));
                }
                /* 1行ずつ読み取りBOCCOに送信 */
                story = br.readLine();
            }
            br.close();
            System.out.println("送信終了");
            /* 終了 */
            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* BOCCOに文字列を送る */
    public static void sendBoccoText(String str) throws Exception {
        boccoAPI.postMessage(str);
        Thread.sleep(5000);
    }

    /* 強制的にフェイズを終わらせる */
    public void setPhraseEnd(boolean flag) {
        phraseEnd = flag;
    }

    /* 録音した文字列を一時保存 */
    public void setStory(String str) {
        saveStory = str;
    }

    /* パス上にある音をならす */
    public static void soundPlay(String soundPath) throws InterruptedException {
        Speaker speaker = new Speaker();
        try {
            speaker.openFile(soundPath);
            speaker.playSE();
            speaker.stopSE();
        } catch (Exception e) {
        }
    }

    /* 直近の録音文字列格納用 */
    public String getStory() {
        return saveStory;
    }

    /* ぶぶー */
    public String getBuzzer() {
        return buzzer;
    }

}
