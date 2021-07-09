//
// Created by huzongyao on 2019/11/12.
//

#include "SeetaDetector.h"

using namespace seeta;

SeetaDetector::SeetaDetector(const char *fdPath, const char *flPath) {
    ModelSetting::Device device = ModelSetting::CPU;
    int id = 0;
    ModelSetting FD_model(fdPath, device, id);
    ModelSetting FL_model(flPath, device, id);
    // 人脸检测
    FD = new FaceDetector(FD_model);
    // 关键点定位
    FL = new FaceLandmarker(FL_model);
    // 防止人脸检测框抖动
    FD->set(FaceDetector::PROPERTY_VIDEO_STABLE, 1);
    FD->set(FaceDetector::PROPERTY_THRESHOLD1, 0.65f);
}

SeetaDetector::~SeetaDetector() {
    delete FD;
    delete FL;
}

SeetaFaceInfoArray SeetaDetector::detect(SeetaImageData &image) {
    return FD->detect(image);
}

std::vector<SeetaPointF> SeetaDetector::mark81(SeetaImageData &image) {
    auto faces = FD->detect(image);
    std::vector<SeetaPointF> points;
    if (faces.size > 0) {
        auto face = faces.data[0];
        // the pos is a matrix limit
        points = FL->mark(image, face.pos);
    }
    return points;
}

void SeetaDetector::pushCorners(SeetaImageData &image, std::vector<SeetaPointF> &points) {
    int width = image.width, height = image.height;
    float r = width - 1.0f, b = height - 1.0f;
    float hr = width / 2.f - 1.f;
    float hb = height / 2.f - 1.f;

    points.push_back({0, 0});
    points.push_back({hr, 0});
    points.push_back({r, 0});
    points.push_back({r, hb});
    points.push_back({r, b});
    points.push_back({hr, b});
    points.push_back({0, b});
    points.push_back({0, hb});
}
