from piq import psnr, ssim
import torch
import os
from PIL import Image
import numpy as np
from torchvision import transforms
import matplotlib.pyplot as plt
import lpips
import argparse
from collections import defaultdict
import json

def read_img(path, metric='psnr'):
    image = Image.open(path).convert('RGB') 
    transform = transforms.ToTensor()
    image = transform(image)  # Converts to [0, 1]
    if metric == 'lpips':
        image = (image - 0.5) * 2  # Scales to [-1, 1]
    return image.unsqueeze(0)
    
def plot_graph(data, kwargs, setup, fig_type='line', multi_images = True):
    title = f"{data['metric']} of {kwargs['location']}_{kwargs['distance']}m_{kwargs['motion_pattern']}_{kwargs['depth']}m_{kwargs['direction']}_{kwargs['gap']}_{data['input_types'][0]}_{data['input_types'][1]}"
    if multi_images == False and fig_type == 'line':
        plot_line(data, title, setup)
    # elif multi_images == True and fig_type == 'point':
    #     plot_points(data, title, setup)
    elif multi_images == True and fig_type == 'box':
        plot_box(data, title, setup)
    else: 
        raise Exception("Plot unavailable. Either change multi_images or fig_type.")

def plot_points(data, title, setup):
    metric = data['metric']
    schemes = data['schemes']
    input_types = data['input_types']
    
    x = np.arange(len(schemes)) 

    plt.figure(figsize=(12, 8))

    ax = plt.gca() 

    for i, scheme in enumerate(schemes):
        for j, input_type in enumerate(input_types):
            ax.scatter(i, data[f'{metric}_val'][scheme][j], marker='o', label=f"{scheme} - {input_type}" if i == 0 else "")  # Only add label for the first x tick

    ax.set_xlabel('schemes')
    ax.set_ylabel(f'{metric}')
    ax.set_title(title)
    ax.set_xticks(x)
    ax.set_xticklabels(schemes)
    ax.legend()

    plt.grid(True)
    plt.savefig(f"underwater_figures/line_{metric}_by_{setup}.png")
    plt.show()

def plot_line(data, title, setup):
    for scheme in data['schemes']:
        tmp = []
        for key in data['psnr_val'][scheme].keys():
            tmp.extend(data['psnr_val'][scheme][key])
        data['psnr_val'][scheme] = tmp
    
    metric = data['metric']
    x = np.arange(len(data['x_labels']))  # the label locations
    _, ax = plt.subplots(figsize=(12, 8))
    for scheme in data['schemes']:
        ax.plot(x, data[f'{metric}_val'][scheme], marker='o', label=scheme)

    ax.set_xlabel(setup.capitalize())
    ax.set_ylabel(f'{metric}')
    ax.set_title(title)
    ax.set_xticks(x)
    ax.set_xticklabels(data['x_labels'])
    ax.legend()

    plt.grid(True)
    plt.savefig(f"underwater_figures/line_{metric}_by_{setup}.png")
    plt.show()

def plot_box(data, title, setup_label):
    data_points = [[] for _ in range(len(data['schemes']))]

        
    colors = ['blue', 'green', 'red', 'purple']
    x_labels = data['x_labels']
    labels = data['schemes']
    metric = data['metric']
    
    for i, scheme in enumerate(data['schemes']):
        for setup in data['x_labels']:
            data_points[i].append(data[f'{metric}_val'][scheme][setup])
    
    fig, ax = plt.subplots()


    positions = np.array(range(len(x_labels))) * (len(labels) + 1) + 1
    width = 0.6
    
    for i, (color, label) in enumerate(zip(colors, labels)):
        psnr_data = [data_points[i][j] for j in range(len(x_labels))]
        pos = positions + i - 1.5
        bp = ax.boxplot(psnr_data, positions=pos, widths=width, patch_artist=True)
        
        for box in bp['boxes']:
            box.set(facecolor=color)
        for whisker in bp['whiskers']:
            whisker.set(color=color)
        for cap in bp['caps']:
            cap.set(color=color)
        for median in bp['medians']:
            median.set(color=color)
        for flier in bp['fliers']:
            flier.set(markerfacecolor=color, marker='o', alpha=0.75)

    ax.legend([plt.Line2D([0], [0], color=color, lw=4) for color in colors], labels, loc='upper right')

    ax.set_xticks(positions)
    ax.set_xticklabels(x_labels)
    
    # Set the title and labels
    ax.set_title(title)
    ax.set_xlabel(setup_label.capitalize())
    ax.set_ylabel(metric.upper())

    # Save the plot
    plt.savefig(f"underwater_figures/box_{metric}_by_{setup_label}.png")
    plt.show()

def load_config_from_json(file_path):
    with open(file_path, 'r') as file:
        config = json.load(file)
    return config

def parse_arguments():
    parser = argparse.ArgumentParser(description='Process some inputs.')
    parser.add_argument('--config', type=str, required=True, help='Path to the JSON config file.')
    
    args = parser.parse_args()
    
    config = load_config_from_json(args.config)
    
    # Extracting the values
    metric = config['metric']
    base_folder = config['base_folder']
    setup = config['setup']
    input_metadata = config['input_metadata']
    fig_type = config['fig_type']
    multi_images = config['multi_images']
    kwargs = config['kwargs']
    selected_images = config['selected_images']

    return metric, base_folder, setup, input_metadata, fig_type, multi_images, kwargs, selected_images

def lpips_function(img0, img1, net='alex'):
    img0, img1 = img0.to(device), img1.to(device)
    loss_fn = lpips.LPIPS(net=net)
    loss_fn.to(device)
    return loss_fn.forward(img0,img1)

def calculate_metric(setup, metric, base_folder, fig_type, input_metadata, kwargs, multi_images = True, selected_images=[]):

    schemes = ['css', 'ofdm_adapt', 'ofdm_wo_adapt', 'proposed']

    functions = {
        'psnr': psnr,
        'ssim': ssim,
        'lpips': lpips_function
    }

    data = {'x_labels': setups[setup], 'schemes': schemes, 'metric': metric, f'{metric}_val': {scheme:defaultdict(list) for scheme in schemes}, 'input_types': input_metadata}

    if selected_images == []:
        image_folders = [folder for folder in os.listdir(base_folder) if os.path.isdir(os.path.join(base_folder, folder))]
    else: 
        image_folders = selected_images

    for image_folder in image_folders:   # image1
        ground_truth_path = os.path.join(base_folder, image_folder, 'groundtruth', type_to_filename[input_metadata[0]])
        ref_img = read_img(ground_truth_path, metric)
        for s in setups[setup]:
            kwargs[setup] = s
            for scheme in schemes:
                folder_name = f"{kwargs['location']}_{kwargs['distance']}m_{kwargs['motion_pattern']}_{kwargs['depth']}m_{kwargs['direction']}_{kwargs['gap']}"
                scheme_folder = os.path.join(base_folder, image_folder, scheme, folder_name)
                if not os.path.isdir(scheme_folder):
                    print(scheme_folder)
                    print(f'{metric}_val')
                    print(scheme)
                    data[f'{metric}_val'][scheme].append(0)
                    continue
                for sample_folder in os.listdir(scheme_folder):
                    if sample_folder == '.DS_Store':
                        continue
                    sample_path = os.path.join(scheme_folder, sample_folder, type_to_filename[input_metadata[1]])
                    if os.path.isfile(sample_path):
                        sample_img = read_img(sample_path, metric)
                        metric_val = functions[metric](ref_img, sample_img).item()
                        data[f'{metric}_val'][scheme][s].append(metric_val)
                    else: 
                        data[f'{metric}_val'][scheme][s].append(0)
        if not multi_images:
            plot_graph(data, kwargs, setup, fig_type, multi_images)    # one graph one image folder, y axis = metric, x axis = setup
    if multi_images:
        plot_graph(data, kwargs, setup, fig_type, multi_images) # y axis = metric, x axis = schemes

if __name__ == '__main__':

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    
    setups = {
        'location': ['air'],
        'distance': [1],
        'motion_pattern': ['static', 'slow', 'fast'],
        'depth': [1, 2, 5],
        'direction': [0, 30, 60],
        'gap': [0,5,10, 25,50]
    }

    type_to_filename = {
            "raw": "Alice-Raw_Input_Bitmap-0.png",
            "sent": "Alice-Sent_Gt_Bitmap-0.png",
            "received": "Bob-Received_Bitmap-0.png",
            "recovered": "Bob-Recovered_Bitmap-0.png"
        }
    
    metric, base_folder, setup, input_metadata, fig_type, multi_images, kwargs, selected_images = parse_arguments()

    if input_metadata[0] not in ['raw', 'sent']:
        raise Exception("Please enter a reference type.")
    if input_metadata[1] not in ['received', 'recovered']:
        raise Exception("Please enter a sample type.")

    if metric not in ['psnr', 'ssim', 'lpips']:
        raise Exception("Please enter a valid metric from ['psnr', 'ssim', 'lpips'].")

    if setup not in setups.keys():
        raise Exception("Unrecognized setup type.")
    
    if not os.path.exists('underwater_figures'):
        os.mkdir('underwater_figures')
    
    calculate_metric(setup, metric, base_folder, fig_type, input_metadata, kwargs, multi_images, selected_images)

    
