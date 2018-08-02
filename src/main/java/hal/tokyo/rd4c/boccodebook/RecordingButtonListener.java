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
    private static final String rec_start = "sound/rec_sta.wav";
    private static final String rec_finish = "sound/rec_fin.wav";

    public RecordingButtonListener(String GoogleAPIKey) {
        this.pushCnt = 0;
        this.microPhone = new MicroPhone();
        this.googleAPI = new GoogleSpeechAPI(GoogleAPIKey);
        this.main = new Main();

        System.out.println("録音ボタンが呼ばれたよ");
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {
        TextMessage textMessage = new TextMessage();

        if (gpdsce.getState() == PinState.HIGH) {
            try {
                this.pushCnt++;
                this.microPhone.init();

                /* 録音開始音 */
                this.main.soundPlay(rec_start);
                /* 約10秒録音　↑との時間差が１～２秒 */
                this.microPhone.startRec();
                this.microPhone.stopRec();
                /* 録音終了音 */
                this.main.soundPlay(rec_finish);

                /* 引数：ファイル名　.wavに変換 */
                File recData = this.microPhone.convertWav("voice");
                this.googleAPI.setFilePATH(recData.getPath());

                /* 変換後文字列格納変数 */
                String result = this.googleAPI.postGoogleAPI();

                /*    音声認識が正常にされた場合    */
                if (result != null) {
                    /* 空白があるとBoccoが喋らなくなるため空白を句読点に変換 */
                    result = result.replace("　", "、");
                    result = result.replace(" ", "、");
                    /* 正常に録れたことをBoccoへ送信 */
                    this.main.sendBoccoText(textMessage.readText(TextMessage.OK_REC));
                    System.out.println("変換文字列:" + result);
                    /* ここで録音データを一時保存 =>ステップボタンが押されたら確定でファイルに書き込み */
                    this.main.setStory(result);
                } else {
                    /* うまく読み込めなかった場合 BOCCOにメッセージ送信 */
                    this.main.sendBoccoText(textMessage.readText(TextMessage.NOT_REC));
                }

                /*    再生ボタンとステップボタンのリスナーをセット    */
                System.out.println("pushCnt == 1");
                this.main.setListener();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
