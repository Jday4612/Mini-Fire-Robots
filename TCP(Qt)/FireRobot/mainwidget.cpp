#include "mainwidget.h"
#include "ui_mainwidget.h"

MainWidget::MainWidget(QWidget *parent)
    : QWidget(parent)
    , ui(new Ui::MainWidget)
{
    ui->setupUi(this);
    //Tab1
    pTab1Camera = new Tab1Camera(ui->pTab1);
    ui->pTab1->setLayout(pTab1Camera->layout());
}

MainWidget::~MainWidget()
{
    delete ui;
}

