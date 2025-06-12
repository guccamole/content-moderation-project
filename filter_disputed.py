import json
from typing import Dict, Any
import os

def is_disputed(tweet_data: Dict[str, Any]) -> bool:
    labels = [annotator['label'] for annotator in tweet_data['annotators']]
    return not all(label == labels[0] for label in labels)

def main():
    input_file = 'data/hatexplain/dataset.json'
    output_file = 'data/hatexplain/disputed_dataset.json'
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    
    with open(input_file, 'r') as f: # takes me back to 106a lol
        dataset = json.load(f)
    
    disputed_tweets = {
        tweet_id: tweet_data 
        for tweet_id, tweet_data in dataset.items() 
        if is_disputed(tweet_data)
    }
    
    with open(output_file, 'w') as f:
        json.dump(disputed_tweets, f, indent=2)

if __name__ == "__main__":
    main()
