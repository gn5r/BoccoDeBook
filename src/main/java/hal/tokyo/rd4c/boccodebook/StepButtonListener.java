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

            switch (this.mode) {
                case "cardSetEnd": {
                    try {
                        /* カード認識＆カード判定に進む */
                        this.main.cardJudge(stage);

                        /* 録音リスナーセット */
                        this.main.gamePlay();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

                /* 終わりのカードか判断する。終わりならwhileから抜ける */
                case "recVoiceEnd": {
                    /* ボタン開放 */
                    this.main.removeStepListener();
                    this.main.removePlayListener();
                    this.main.removeRecListener();                    

                    this.main.BGMStop();
                    this.main.endJudge();
                    break;
                }
                default:
                    break;
            }
        }

    }
}
