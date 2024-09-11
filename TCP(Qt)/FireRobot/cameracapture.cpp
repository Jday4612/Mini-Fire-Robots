#include "cameracapture.h"
int Cameracapture::RED=1;
int Cameracapture::GREEN=2;
int Cameracapture::BLUE=3;
Cameracapture::Cameracapture(QWidget *parent) :  QThread(parent)
{
    camFlag = false;
    saveFlag = false;
    timerFlag = false;
    intruderFlag = false;
    colorMode = false;
    frameCount = 0;
    mColor = 0;
    waitTime = 1000; //1000ms
    pQTimer = new QTimer(this);
    connect(pQTimer, SIGNAL(timeout()),this,SLOT(slotMScounter()));
}
void Cameracapture::slotMScounter() {
    timerFlag=true;
}
void Cameracapture::camTimerStartStop(bool check) {
    if(check)
       pQTimer->start(waitTime);
    else if(pQTimer->isActive())
       pQTimer->stop();
}
void Cameracapture::run()
{
    QString text;
    VideoCapture  capture(0);
    if (!capture.isOpened())
    {
        cout << "카메라가 연결되지 않았습니다." << endl;
        exit(1);
    }

    while(camFlag) {
        capture.read(frame);
        if(colorMode)
            colorProcess();
        put_string(frame, "count : ", Point(10, 40), frameCount++);
        cvtColor(frame, frameQt, COLOR_BGR2RGB);
        QImage _img(frameQt.data, frameQt.cols, frameQt.rows, QImage::Format_RGB888);
        line(frameQt, Point((frameQt.cols >> 1) - 15, frameQt.rows >> 1), Point((frameQt.cols >> 1) + 15, frameQt.rows >> 1), Scalar(255, 0, 0), 2);
        line(frameQt, Point(frame.cols >> 1, (frameQt.rows >> 1) - 15), Point(frameQt.cols >> 1, (frameQt.rows >> 1) + 15), Scalar(255, 0, 0), 2);
        pTLcamView->setPixmap(QPixmap::fromImage(_img));

        if(timerFlag)
        {
             securityProcess();
             timerFlag = false;

        }

    }
    capture.release();
//    destroyWindow("카메라 영상보기");
}

void Cameracapture::put_string(Mat &frame, string text, Point pt, uint32_t value)
{
    text += to_string(value);
    Point shade = pt + Point(2, 2);
    int font = FONT_HERSHEY_SIMPLEX;
    putText(frame, text, shade, font, 0.7, Scalar(0, 0, 0), 2);     // 그림자 효과
    putText(frame, text, pt, font, 0.7, Scalar(120, 200, 90), 2);// 작성 문자
}

bool Cameracapture::getCamFlag()
{
    return camFlag;
}
void Cameracapture::setCamFlag(bool camFlag)
{
    this->camFlag = camFlag;
}
void Cameracapture::camSnapshot()
{
//    cvtColor(frame, frame, COLOR_RGB2BGR);
    imwrite("capture.jpg", frame);
}

void Cameracapture::securityProcess()
{
    int pixelCount = 0;
    if(!saveFlag)
    {
        securityFrame = frameQt.clone();
        saveFlag = true;
        if(intruderFlag)
        {
            emit sigSocketSendData("[HM_CON]GASOFF");
            intruderFlag = false;
        }
    }
    else
    {
        securityFrame = abs(securityFrame - frameQt);
        uchar* my_ptr = (uchar*)securityFrame.data; // v+1행 첫 칸의 주소를 불러온다.
        for(int v= 0;v<securityFrame.rows ; v++){
            for(int u = 0 ; u<securityFrame.cols; u++){
                if(my_ptr[v*securityFrame.cols + u] >=30) pixelCount++;
//                   if(pixelCount > 3072)  //1%
 //                      break;
            }
//          if(pixelCount > 3072)
//              break;
        }
        if(pixelCount > 3072)
        {
//            cvtColor(frame, frame, COLOR_RGB2BGR);
            imwrite("security.jpg", frame);
            emit sigSocketSendData("[HM_CON]INTRUDER");
            QThread::msleep(200);
            emit sigSocketSendData("[HM_CON]GASON");
            qDebug() << "INTRUDER " << pixelCount ;
            intruderFlag = true;
         }
         saveFlag = false;
    }
}
void Cameracapture::colorProcess()
{
    Mat img_hsv;
    cvtColor(frame, img_hsv, COLOR_BGR2HSV);

    uchar* pointer_row = img_hsv.ptr<uchar>(img_hsv.rows >> 1);

    uchar h = pointer_row[(img_hsv.cols >> 1) * 3 + 0];
    uchar s = pointer_row[(img_hsv.cols >> 1) * 3 + 1];
    uchar v = pointer_row[(img_hsv.cols >> 1) * 3 + 2];




    if((mColor != Cameracapture::RED) && (0 <= h && h <= 10))            //red
    {
        emit sigSocketSendData("[KSH_LIN]COLOR@RED");
        mColor = Cameracapture::RED;
        qDebug() << " h:" << h << " s:" << s <<" v:" <<v << endl;
    }
    else if((mColor != Cameracapture::GREEN) && (50 <= h && h <= 70))         //green
    {
        emit sigSocketSendData("[KSH_LIN]COLOR@GREEN");
        mColor = Cameracapture::GREEN;
        qDebug() << " h:" << h << " s:" << s <<" v:" <<v << endl;
    }
    else if((mColor != Cameracapture::BLUE) && (100 <= h && h <= 120))       //blue
    {
        emit sigSocketSendData("[KSH_LIN]COLOR@BLUE");
        mColor = Cameracapture::BLUE;
        qDebug() << " h:" << h << " s:" << s <<" v:" <<v << endl;
    }
}
