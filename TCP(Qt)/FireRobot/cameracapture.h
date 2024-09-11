#ifndef CAMERACAPTURE_H
#define CAMERACAPTURE_H
#include <QThread>
#include <QWidget>
#include <QLabel>
#include <QDebug>
#include <QMessageBox>
#include <QTimer>
#include <QLabel>
#include <QTimer>
#include <QDebug>
#include <opencv2/opencv.hpp>
#include <iostream>
using namespace cv;
using namespace std;

class Cameracapture : public QThread
{

    Q_OBJECT
    void run();
    int camFlag;
    int saveFlag;
    int waitTime;
    int frameCount;
    bool intruderFlag;
    Mat frame, frameQt;
    Mat securityFrame;
    void put_string(Mat &frame, string text, Point pt, uint32_t value);
    QTimer* pQTimer;
    bool timerFlag;
    int hue, sat, val, conval, robval, cnt;
    int mColor;
public:
    static int RED;
    static int GREEN;
    static int BLUE;
    bool colorMode;
public:
    explicit Cameracapture(QWidget *parent = nullptr);
    bool getCamFlag();
    void setCamFlag(bool);
    void showImage(Mat img);
    void securityProcess();
    QLabel* pTLcamView;
    void camTimerStartStop(bool);
    void camSnapshot();
    void colorProcess();
signals:
    void sigSocketSendData(QString);
public slots:
    void slotMScounter();
};
#endif // CAMERACAPTURE_H
