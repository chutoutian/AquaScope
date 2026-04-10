<div align="center">

# AquaScope: Reliable Underwater Image Transmission on Mobile Devices

[📄 Paper (arXiv)](https://arxiv.org/abs/2502.10891) | [🌐 Project Website](https://chutoutian.github.io/aquascope.github.io/) | [📁 Data & Checkpoints (Google Drive)](https://drive.google.com/file/d/1zg30JxvskZU5_nYn1bSiV0O40uyTQa9b/view?usp=sharing)

</div>

---

## 🧭 Overview

**AquaScope** is the first underwater communication system that enables image transmission on commodity mobile devices which leverages the generative image codecs. 

---

## 🚧 Project Status

We are actively improving this repository.

### ✅ Done

- [x] Released andriod code
- [x] Released checkpoints
- [x] Update usage instructions
- [x] Provide [scripts and intermediate artifacts](https://drive.google.com/file/d/1ReZj-Afcy2YXzlZkiSbVr95FOwAh-oYQ/view?usp=sharing) to reproduce the experimental results in the paper


### 🔜 Upcoming

- [ ] Release offline processing code (matlab)
- [ ] Release fine-tuning codes and instructions


## ⚙️ Quick Start on Android Phone

> **Requirements:** Android Studio Arctic Fox 2022.3.1 Patch 1 · Tested on Samsung Galaxy S21

---

### Step 1: Clone the Repository

```bash
git clone https://github.com/chutoutian/AquaScope.git
cd AquaScope
git checkout dev_new_model
```

---

### Step 2: Build and Run the App

1. Open the project (AquaScope) in **Android Studio**.
2. Connect your Android phone with **USB debugging** enabled.
3. Copy all [checkpoint](https://drive.google.com/file/d/1zg30JxvskZU5_nYn1bSiV0O40uyTQa9b/view?usp=sharing) files to `/data/data/com.example.aquascope/files` via **Device Manager**.
4. Build and run the app on the phone.

---

### Step 3: Run an Experiment

Choose one of the two modes below.

#### 🔹 Option A — Running Example (`testExp`)

Set **ExpMode** to `testExp`, then follow the steps for each role:

| Role | Steps |
|------|-------|
| **Sender** | 1. Select **Encode Image** → 2. Click **Run Model** → 3. Click **Send** *(listen for transmission sound)* |
| **Receiver** | 1. Select **Decode Image** → 2. Wait for the image to be received and displayed |

#### 🔹 Option B — Scaled Experiment (`end2endTest`)

Set **ExpMode** to `end2endTest`, then:

1. Click **Ready**.
2. Configure the parameters in the pop-up UI.
3. Click **Finish** after setup is complete.

---

## 📊 Reproducing Paper Results

1. Download the scripts and dataset (collected from the real world) from [Google Drive](https://drive.google.com/file/d/1ReZj-Afcy2YXzlZkiSbVr95FOwAh-oYQ/view?usp=sharing).
2. Navigate to each folder and run the corresponding script to reproduce the results


## 🙏 Acknowledgments

This project builds on top of [AquaApp](https://github.com/uw-x/watercomms).

---

## 📜 License

This project is released under the [MIT License](LICENSE).

---

## 🔗 Citation

If you use our work, please cite:

```bibtex
@misc{tian2025aquascopereliableunderwaterimage,
  title         = {AquaScope: Reliable Underwater Image Transmission on Mobile Devices},
  author        = {Beitong Tian and Lingzhi Zhao and Bo Chen and Mingyuan Wu and Haozhen Zheng and Deepak Vasisht and Francis Y. Yan and Klara Nahrstedt},
  year          = {2025},
  eprint        = {2502.10891},
  archivePrefix = {arXiv},
  primaryClass  = {cs.NI},
  url           = {https://arxiv.org/abs/2502.10891},
}


