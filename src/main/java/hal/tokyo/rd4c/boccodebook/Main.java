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
import hal.tokyo.rd4c.speech2text.MicroPhone;
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
    /* 起動音のパス */
    private static final String setupSoundPath = "setup/setup.wav";
    /* 物語格納用のパス */
    private static final String txtFilePath = "text/story.txt";

    private static MicroPhone microPhone;
    private static Speaker speaker;

    /* BOCCOと接続用String */
    private static String GOOGLE_API_KEY;

    /* BGM番号 */
    private static int BGMNum = 0;
    private static BGMPlayer BPlayer;
    /* ステージ回数0-3 */
    private static int stage = 0;
    /* 終わりflag */
    private static boolean endFlag = false;

    private static final int CUT_LENGTH = 150;

    /* BoocoAPI(String APIKey, String Email, String PassWord) */
    private static BoccoAPI boccoApi;
    private static TextMessage textMessage;

    /* 変換後文字列を格納するためのファイル名を格納 */
    public static String recFileName;

    /* GPIO */
    private static GpioPinDigitalInput startSE, event1SE, event2SE, endingSE, step, play, rec;
    private static GpioPinDigitalOutput startSELED, event1SELED, event2SELED, endingSELED;
    private static GpioController gpio;

    /* メイン */
    public static void main(String[] args) throws Exception {

        /*初期化 */
        init(args);

        while (true) {
            /* mode = CardSet & stepボタンのリスナー設定 */
            cardSet(stage);

            /* 終わりカードがセットされていない間繰り返す */
            while (!endFlag) {
                Thread.sleep(500);
            }
            /* BOCCOに今までの文字列を送信 */
            sendStory();
            /* 終了 */
            return;
        }
    }

    /* 初期化 */
    private static void init(String[] args) throws Exception {

        /* 引数の数が正しくない場合終了 */
        if (args.length != 4) {
            return;
        }

        /* args[0]:BOCCOAPI args[1]:Email args[2]:PassWord args[3]:GOOGLE_API_KEY */
        boccoApi = new BoccoAPI(args[0], args[1], args[2]);
        GOOGLE_API_KEY = args[3];
        microPhone = new MicroPhone();
        speaker = new Speaker();
        textMessage = new TextMessage();

        microPhone.init();

        /* 起動音を鳴らす */
        speaker.openFile(setupSoundPath);
        speaker.playSE();
        speaker.stopSE();

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

        /* BOCCOの接続が確立できた場合 */
        if (boccoApi.createSessions() == true) {
            boccoApi.getFirstRooID();
            boccoApi.postMessage(textMessage.readText(TextMessage.SESSION_OK));
        }
    }

    /* どのカードをセットするかBOCCOに喋ってもらう */
    public static void cardSet(int stage) throws Exception {

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

        /* ステップ押下の誘導メッセージ */
        sendText += textMessage.readText(TextMessage.CARD_STEP);
        boccoApi.postMessage(sendText);

        /* カードセットが終了 */
        mode = "cardSetEnd";

        /* stepボタンのリスナーをセット => modeごとに動作を変更 */
        step.addListener(new StepButtonListener(stage, mode, boccoApi));
    }

    /* NFCリーダーでデータを読み込む.   false:エラー   0 - 14:正常 */
    private static int cardScan() throws Exception {
        nfcReader = new NFCReader();
        int BGMNum = ERROR;
        /* 正常な値が読み取れるまで */
        while (BGMNum == ERROR) {
            BGMNum = nfcReader.readBGMNum();
            Thread.sleep(1000);
        }
        return BGMNum;
    }

    /* カード番号取得後、正しいカードか判断 & 正しいカードならmodeを更新 */
    public boolean cardJudge(int stage) throws Exception {
        /* カード番号取得 */
        BGMNum = cardScan();
        /* 異常値：false, 正常値：true */
        boolean flag = false;

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
                        /* 前のLEDを消灯 */
                        startSELED.low();
                        /* SEボタンセット */
                        event1SE.addListener(new SEButtonListener(event1SELED, BGMNum));
                    } else if (stage == 2) {
                        /* 前のLEDを消灯 */
                        event1SELED.low();
                        /* SEボタンセット */
                        event2SE.addListener(new SEButtonListener(event2SELED, BGMNum));
                    }
                }
                break;
            /* 四枚目 */
            case 3:
                if (12 <= BGMNum && BGMNum < 15) {
                    flag = true;
                    /* 前のLEDを消灯 */
                    event2SELED.low();
                    /* SEボタンセット */
                    endingSE.addListener(new SEButtonListener(endingSELED, BGMNum));
                }
                break;
            /* 例外処理 */
            default:
                System.out.println("想定外の数値が入力されました。");
                break;
        }

        /* リスナーの削除 */
        step.removeAllListeners();

        /* 録音ボタンのリスナーを設定 */
        return flag;
    }

    /* ゲーム実行時呼ばれる */
    public void gamePlay() throws Exception {
        BPlayer = new BGMPlayer(BGMNum);
        BPlayer.start();
        rec.addListener(new RecordingButtonListener(GOOGLE_API_KEY));
    }

    /* BGMの終了(録音終了のstep押下時) */
    public void BGMStop() {
        BPlayer.stopBGM();
    }

    /* stepとplayのリスナーを設定する */
    public void setListener() {
        step.addListener(new StepButtonListener(stage, mode, boccoApi));
        play.addListener(new PlayButtonListener(BPlayer));
    }

    /* 変換後文字列を格納するためのtxtファイル */
    private static void createFile(String filePath) throws IOException {
        File recorFile = new File(filePath);
        try {
            if (recorFile.createNewFile()) {
                System.out.println("ファイルの作成に成功しました。");
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /* 音声->文字列化したものをファイルに書き込み */
    public void writeFile(String data) throws IOException {
        /* story.txt */
        File file = new File(txtFilePath);

        try {
            /* 書き込みが可能ならば */
            if (checkBeforeWritefile(file)) {
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                /* 文字列の書き込み & 改行して保存 */
                pw.println(data);
                /* ファイルを閉じる */
                pw.close();
            } else {
                System.out.println("ファイルに書き込めません");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /* 再生ボタンが押されたときの挙動 */
    public void recentlySend() throws Exception{
        String returnData = "";
        
        try {
            File file = new File(txtFilePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            returnData = br.readLine();
            /* 文字列全てから必要なものだけ抽出する */
            while (returnData != null) {
                System.out.print(returnData);
            }
            returnData = br.readLine();
            br.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        /* 直近のデータを送信 */
        boccoApi.postMessage(returnData);
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
        if (11 < BGMNum && BGMNum <= 14) {
            endFlag = true;
        }
        /* 次のステージ（カード）へ移行 */
        stage++;
    }

    /* BOCCOに今までの物語を送信する txt名:txtFilePath */
    private static void sendStory() throws Exception {
        String story = "";
        File file = new File(txtFilePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        int cnt = 0;

        try {
            story = br.readLine();
            /* 全ての列を網羅 */
            while (story != null) {
                /* BOCCOに送れる範囲なら送信 */
                if (story.length() < CUT_LENGTH) {
                    boccoApi.postMessage(story);
                } else {
                    /* CUT_LENGTHより長いならCUT_LENGTHずつ分けてBOCCOへ送信 */
                    for (cnt = 0; cnt + CUT_LENGTH < story.length(); cnt += CUT_LENGTH) {
                        boccoApi.postMessage(story.substring(cnt, cnt + CUT_LENGTH));
                    }
                    Thread.sleep(2000);
                    /* 最後の行 */
                    boccoApi.postMessage(story.substring(cnt));
                }
                /* 1行ずつ読み取りBOCCOに送信 */
                story = br.readLine();
                /* BOCCOの読み待ち */
                Thread.sleep(2000);
            }
            br.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }
}
