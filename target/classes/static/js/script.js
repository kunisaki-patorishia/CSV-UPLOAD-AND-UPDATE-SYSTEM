document.addEventListener('DOMContentLoaded', function() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');
    const uploadForm = document.getElementById('uploadForm');
    const uploadBtn = uploadForm.querySelector('.upload-btn');
    const selectedFileName = document.getElementById('selectedFileName');

    // Drag and drop functionality
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
    });

    ['dragleave', 'dragend'].forEach(type => {
        uploadArea.addEventListener(type, (e) => {
            e.preventDefault();
            uploadArea.classList.remove('drag-over');
        });
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            handleFileSelection(files[0]);
        }
    });

    // File input change
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFileSelection(e.target.files[0]);
        }
    });

    function handleFileSelection(file) {
        if (file && file.name.toLowerCase().endsWith('.csv')) {
            selectedFileName.textContent = `Selected: ${file.name}`;
            selectedFileName.style.display = 'block';
            uploadBtn.disabled = false;
            fileInput.files = [file]; // Update the form file input
        } else {
            alert('Please select a CSV file');
            resetFileSelection();
        }
    }

    function resetFileSelection() {
        selectedFileName.style.display = 'none';
        uploadBtn.disabled = true;
        fileInput.value = '';
    }

    // Form submission
    uploadForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const file = fileInput.files[0];
        if (!file) {
            alert('Please select a file');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        try {
            uploadBtn.disabled = true;
            uploadBtn.textContent = 'Uploading...';

            const response = await fetch('/upload', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (response.ok) {
                alert('File uploaded successfully! Processing in background.');
                resetFileSelection();
                refreshUploads();
            } else {
                alert('Upload failed: ' + (result.error || 'Unknown error'));
            }
        } catch (error) {
            alert('Upload error: ' + error.message);
        } finally {
            uploadBtn.disabled = false;
            uploadBtn.textContent = 'Upload File';
        }
    });

    // Real-time updates
    async function refreshUploads() {
        try {
            const response = await fetch('/api/uploads');
            const uploads = await response.json();
            
            const table = document.getElementById('uploadsTable');
            table.innerHTML = uploads.map(upload => `
                <tr>
                    <td>${new Date(upload.createdAt).toLocaleString()}</td>
                    <td>${upload.fileName}</td>
                    <td>
                        <span class="status status-${upload.status}">${upload.status}</span>
                    </td>
                    <td>${upload.processedRows || 0}</td>
                </tr>
            `).join('');
        } catch (error) {
            console.error('Error refreshing uploads:', error);
        }
    }

    // Initial load and periodic refresh
    refreshUploads();
    setInterval(refreshUploads, 3000); // Refresh every 3 seconds
});


async function compareRecord() {
    const uniqueKey = document.getElementById('uniqueKeyInput').value.trim();
    if (!uniqueKey) {
        alert('Please enter a UNIQUE_KEY');
        return;
    }

    try {
        const response = await fetch(`/api/compare/${encodeURIComponent(uniqueKey)}`);
        const result = await response.json();
        
        const comparisonDiv = document.getElementById('comparisonResult');
        
        if (response.ok && result.latest) {
            comparisonDiv.innerHTML = `
                <h4>Comparison for: ${result.uniqueKey}</h4>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 10px;">
                    <div>
                        <strong>Previous Version:</strong>
                        <div>File: ${result.previous.uploadFile}</div>
                        <div>Title: ${result.previous.productTitle}</div>
                        <div>Price: $${result.previous.piecePrice}</div>
                    </div>
                    <div>
                        <strong>Latest Version:</strong>
                        <div>File: ${result.latest.uploadFile}</div>
                        <div>Title: ${result.latest.productTitle}</div>
                        <div>Price: $${result.latest.piecePrice}</div>
                    </div>
                </div>
                ${result.changed ? '<div style="color: green; margin-top: 10px;">✅ Record was updated</div>' : 
                                  '<div style="color: orange;">⚠️ No changes detected</div>'}
            `;
            comparisonDiv.style.display = 'block';
        } else {
            comparisonDiv.innerHTML = `<div style="color: red;">No comparison data found for this key</div>`;
            comparisonDiv.style.display = 'block';
        }
    } catch (error) {
        alert('Error comparing records: ' + error.message);
    }
}

async function validateLatestUpload() {
    try {
        // First get the latest upload
        const uploadsResponse = await fetch('/api/uploads');
        const uploads = await uploadsResponse.json();
        
        if (uploads.length === 0) {
            alert('No uploads found');
            return;
        }
        
        const latestUpload = uploads[0];
        const response = await fetch(`/api/validate/${latestUpload.id}`);
        const result = await response.json();
        
        const validationDiv = document.getElementById('validationResult');
        validationDiv.innerHTML = `
            <div style="background: white; padding: 15px; border-radius: 5px; border-left: 4px solid #667eea;">
                <h4>Validation for: ${latestUpload.fileName}</h4>
                <div>Total Records: ${result.totalUploaded}</div>
                <div>Status: ${latestUpload.status}</div>
                ${result.missingKeys && result.missingKeys.length > 0 ? 
                    `<div style="color: red;">Missing Keys: ${result.missingKeys.join(', ')}</div>` : 
                    '<div style="color: green;">✅ All expected keys found</div>'}
                ${result.sampleRecords ? `
                    <div style="margin-top: 10px;">
                        <strong>Sample Records:</strong>
                        <pre style="background: #f5f5f5; padding: 10px; border-radius: 3px; margin-top: 5px;">
${JSON.stringify(result.sampleRecords, null, 2)}
                        </pre>
                    </div>
                ` : ''}
            </div>
        `;
    } catch (error) {
        alert('Validation error: ' + error.message);
    }
}