/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import hal.tokyo.rd4c.speech2text.GoogleSpeechAPI;
import hal.tokyo.rd4c.speech2text.MicroPhone;
import java.io.File;
import javax.sound.sampled.LineUnavailableException;

/**
 *
 * @author gn5r
 */
public class RecordingButtonListener implements GpioPinListenerDigital {

    private final MicroPhone microPhone;
    private final GoogleSpeechAPI googleAPI;
    private int pushCnt;
    private final Main main;

    public RecordingButtonListener(String GoogleAPIKey) {
        this.pushCnt = 0;
        this.microPhone = new MicroPhone();
        this.googleAPI = new GoogleSpeechAPI(GoogleAPIKey);
        this.main = new Main();

        System.out.println("録音ボタンが呼ばれたよ");
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {
        if (gpdsce.getState() == PinState.HIGH) {

            try {
                this.pushCnt++;
                this.microPhone.init();
                /* 300000byte分の音データ録音 */
                this.microPhone.startRec();
                this.microPhone.stopRec();
                /* 引数：ファイル名　.wavに変換 */
                File recData = this.microPhone.convertWav("voice");
                this.googleAPI.setFilePATH(recData.getPath());

                /* 変換後文字列格納変数 */
                String result = this.googleAPI.postGoogleAPI();
                /*    音声認識が正常にされた場合    */
                if (result != null) {
                    System.out.println("変換文字列:" + result);
                    /* ファイルに書き込み(一行データ)*/
                    this.main.writeFile(result);
                }

                /*    録音ボタンが初めて押されたかどうかの検出    */
                if (this.pushCnt == 1) {
                    /*    再生ボタンとステップボタンのリスナーをセット    */
                    System.out.println("pushCnt == 1");
                    this.main.setListener();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
