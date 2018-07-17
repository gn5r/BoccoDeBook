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
import java.io.File;
import java.io.IOException;

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
    private static final String setupSound = "setup/setup.wav";
    private static MicroPhone microPhone;
    private static Speaker speaker;

    /* BOCCOと接続用String */
    private static String GOOGLE_API_KEY;

    /* BoocoAPI(String APIKey, String EMAIL, String PASSWORD) */
    private static BoccoAPI boccoApi;
    private static TextMessage textMessage;

    /* 変換後文字列を格納するためのファイル名を格納 */
    public static String recFileName;

    /* GPIO */
    private static GpioPinDigitalInput startSE, event1SE, event2SE, endingSE, step;
    private static GpioPinDigitalOutput startSELED, event1SELED, event2SELED, endingSELED;
    private static GpioController gpio;

    /* メイン */
    public static void main(String[] args) throws Exception {
        /* ステージ回数0-3 */
        int stage = 0;

        /*初期化 */
        init(args);
        /* stepボタンのリスナーをセット => modeごとに動作を変更 */
        step.addListener( /* mode 変更メソッド */);

        while (true) {
            Thread.sleep(500);
        }

    }

    /* 初期化 */
    private static void init(String[] args) throws Exception {

        /* args[0]:BOCCOAPI args[1]:Email args[2]:PassWord args[3]:GOOGLE_API_KEY */
        boccoApi = new BoccoAPI(args[0], args[1], args[2]);
        GOOGLE_API_KEY = args[3];
        microPhone = new MicroPhone();
        speaker = new Speaker();

        microPhone.init();
        mode = "CardSet";

        /* 起動音を鳴らす */
        speaker.openFile(setupSound);
        speaker.playSE();
        speaker.stopSE();

        gpio = GpioFactory.getInstance();

        /*    各SEボタンとLED, ピンは仮    */
        startSE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP);
        startSE.setShutdownOptions(true);

        startSELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "startSE", PinState.LOW);
        startSELED.setShutdownOptions(true, PinState.LOW);

        event1SE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_UP);
        event1SE.setShutdownOptions(true);

        event1SELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "event1SE", PinState.LOW);
        event1SELED.setShutdownOptions(true, PinState.LOW);

        event2SE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_26, PinPullResistance.PULL_UP);
        event2SE.setShutdownOptions(true);

        event2SELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27, "Sevent2SE", PinState.LOW);
        event2SELED.setShutdownOptions(true, PinState.LOW);

        endingSE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_28, PinPullResistance.PULL_UP);
        endingSE.setShutdownOptions(true);

        endingSELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, "endingSE", PinState.LOW);
        endingSELED.setShutdownOptions(true, PinState.LOW);

        step = gpio.provisionDigitalInputPin(RaspiPin.GPIO_31, PinPullResistance.PULL_UP);
        step.setShutdownOptions(true);

        /* BOCCOの接続が確立できた場合 */
        if (boccoApi.createSessions() == true) {
            boccoApi.getFirstRooID();
            boccoApi.postMessage(textMessage.readText(TextMessage.SESSION_OK));
        }

        /* ストーリーを格納するファイルの作成 */
        recFileName = createFile();
    }

    /* どのカードをセットするかBOCCOに喋ってもらう */
    private static void cardSet(int stage) throws Exception {
        textMessage = new TextMessage();

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
    public static boolean cardJudge(int stage) throws Exception {
        /* カード番号取得 */
        int BGMNum = cardScan();
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

        /* 正しいカードがセットされていたらモードを更新 */
        if (flag == true) {
            mode = "CardScan";
        }
        return flag;
    }

    /* 変換後文字列を格納するためのtxtファイル */
    private static String createFile() throws IOException {
        String strDate = "Story.txt";
        File recorFile = new File(strDate);
        try {
            if (recorFile.createNewFile()) {
                System.out.println("ファイルの作成に成功しました。");
            } else {
                System.out.println("既に同じ名前のファイルがあります。");
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return strDate;
    }
}
