from flask import Flask, jsonify, request
from flask_cors import CORS
from model import predict_donors, train_model

app = Flask(__name__)
CORS(app)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "BloodLink AI service running", "version": "1.0"})

@app.route('/api/ai/rank-donors', methods=['POST'])
def rank_donors():
    data = request.get_json()
    emergency_id = data.get('emergency_id')

    if not emergency_id:
        return jsonify({"error": "emergency_id is required"}), 400

    try:
        ranked = predict_donors(emergency_id)
        print(f"Flask ranked donors count: {len(ranked)} for emergency {emergency_id}")
        if ranked:
            print(f"Top ranked donor: {ranked[0]}")
        return jsonify({
            "emergency_id":        emergency_id,
            "total_donors_ranked": len(ranked),
            "ranked_donors":       ranked
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/ai/train', methods=['POST'])
def retrain():
    try:
        train_model()
        return jsonify({"message": "Model retrained successfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("Starting BloodLink AI Prediction Service on port 8087...")
    app.run(port=8087, debug=True)