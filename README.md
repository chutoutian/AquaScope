<div align="center">

# AquaScope: Reliable Underwater Image Transmission on Mobile Devices

[📄 Paper (arXiv)](https://arxiv.org/abs/2502.10891) | [🌐 Project Website](https://chutoutian.github.io/aquascope.github.io/) | [📁 Data & Checkpoints (Google Drive)](https://drive.google.com/file/d/1zg30JxvskZU5_nYn1bSiV0O40uyTQa9b/view?usp=sharing)

</div>

---


## 🧭 Overview

**AquaScope** is the first underwater communication system that enables image transmission on commodity mobile devices which leverages the generative image codecs. 

---

> [!IMPORTANT]
> For the latest updates and features, switch to the [`dev_new_model`](https://github.com/chutoutian/AquaScope/tree/dev_new_model) branch:
> ```bash
> git checkout dev_new_model
> ```

---

## 🚧 Project Status

We are actively improving this repository.

### ✅ Done

- [x] Released andriod code
- [x] Released checkpoints
- [x] Update usage instructions
- [x] Provide scripts to reproduce the experimental results in the paper

### 🔜 Upcoming

- [ ] Release offline processing code (matlab)
- [ ] Release fine-tuning codes and instructions


## ⚙️ Quick Start on Android Phone

The project was originally built with Android Studio Arctic Fox 2022.3.1 Patch 1, and has been tested on Samsung Galaxy S21 phones.


### Step 1: Clone the repository

Clone the repository and switch to the `dev_new_model` branch.

```bash
git clone https://github.com/chutoutian/AquaScope.git
cd AquaScope
git checkout dev_new_model
```

### Step 2: Build and run the app

1. Open the project in **Android Studio**.
2. Connect your Android phone and make sure **USB debugging** is enabled.
3. In **Device Manager**, copy all [checkpoint](https://drive.google.com/file/d/1zg30JxvskZU5_nYn1bSiV0O40uyTQa9b/view?usp=sharing) files to `/data/data/com.example.aquascope/files`.
4. Build and run the app on the phone.

### Step 3 (Running Example)


Set **ExpMode** to **testExp**.

#### Sender
1. Choose one device as the sender.
2. Select **Encode Image**.
3. Click **Run Model**.
4. Click **Send**. (You should hear the transmission sound)

#### Receiver
1. Choose the other device as the receiver.
2. Select **Decode Image**.
3. Wait for the image to be received and displayed.

### Step 3 (Scaled Experiment)


1. Set **ExpMode** to **end2endTest**.
2. Click **Ready**.
3. Configure the corresponding parameters in the pop-up UI.
4. Click **Finish** after the setup is complete.

---

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





