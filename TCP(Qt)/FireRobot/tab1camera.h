#ifndef TAB1CAMERA_H
#define TAB1CAMERA_H

#include <QWidget>
#include <QTimer>
#include <QTcpServer>
#include <QTcpSocket>
#include <QImage>
#include <QPixmap>

namespace Ui {
class Tab1Camera;
}

class Tab1Camera : public QWidget
{
    Q_OBJECT

public:
    explicit Tab1Camera(QWidget *parent = nullptr);
    ~Tab1Camera();

private:
    Ui::Tab1Camera *ui;
    QTcpServer *server;
    QTcpSocket *client;
    QTimer *timer;

private slots:
    void slotNewConnection();
    void slotReadData();
    void slotProcessImage();
};

#endif // TAB2CAMERA_H
