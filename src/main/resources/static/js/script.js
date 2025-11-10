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
