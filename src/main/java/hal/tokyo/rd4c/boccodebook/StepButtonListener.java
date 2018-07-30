/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 *
 * @author gn5r
 */
public class StepButtonListener implements GpioPinListenerDigital {

    /*    カードの読取り枚数    */
    private final int stage;
    private final String mode;
    private final Main main;

//    private RaspRC522 rsp;
    public StepButtonListener(int stage, String mode) {
        this.stage = stage;
        this.mode = mode;
        this.main = new Main();
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {

        if (gpdsce.getState() == PinState.LOW) {
            try {
                Thread.sleep(500);
                switch (this.mode) {
                    /* カードを読み込み、正しいなら続け、違うカードをセットされたらフェーズを抜ける */
                    case "cardSetEnd": {
                        try {
                            /* 適切なカードがセットされなかったら */
                            if (this.main.cardJudge(stage) != true) {
                                /* cardSetに戻る為にmainのphraseEndをtrueにする */
                                this.main.setPhraseEnd(true);
                                
                                /* cardSetでstepボタンがaddされるためリスナー解放 */
                                this.main.removeStepListener();
                                
                                /* ブザー音を鳴らす */
                                this.main.soundPlay(this.main.getBuzzer());
                            } else {
                                
                                /* 録音リスナーセット */
                                this.main.gamePlay();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                    /* 終わりのカードか判断する。終わりならwhileから抜ける */
                    case "recVoiceEnd": {
                        try {
                            /* 最後に保存された文章をファイルに保存 */
                            this.main.writeFile(this.main.getStory());

                            /* ボタン開放 */
                            this.main.removeStepListener();
                            this.main.removePlayListener();
                            this.main.removeRecListener();

                            /* BGM終了 */
                            this.main.BGMStop();
                            
                            /* 終わり判定 */
                            this.main.endJudge();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    }
                    default:
                        System.out.println("StepButtonListener :" + this.mode);
                        System.out.println("異常値を検出");
                        break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
