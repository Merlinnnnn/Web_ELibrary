import React, { useState, useEffect } from 'react';
import {
    Box,
    TextField,
    Button,
    Typography,
    Paper,
    Grid,
    Chip,
    Avatar,
    IconButton,
    SpeedDial,
    SpeedDialAction,
    DialogActions,
    DialogContent,
    Dialog,
    DialogTitle,
    Snackbar,
    Alert,
    Card,
    CardContent,
    Divider,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    InputAdornment,
    CircularProgress,
} from '@mui/material';
import Sidebar from '../SideBar';
import apiService from '../../untils/api';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import AddIcon from '@mui/icons-material/Add';
import CloseIcon from '@mui/icons-material/Close';
import SubjectIcon from '@mui/icons-material/Subject';
import LabelIcon from '@mui/icons-material/Label';
import PublishIcon from '@mui/icons-material/Publish';
import DescriptionIcon from '@mui/icons-material/Description';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import ArticleIcon from '@mui/icons-material/Article';
import ImportExcelDialog from '../AddBooks/ImportExcelDiolog';
import { styled } from '@mui/material/styles';

interface Book {
    isbn: string;
    documentName: string;
    author: string;
    publisher: string;
    publishedDate: string;
    pageCount: number;
    language: string;
    quantity: number;
    availableCount: number;
    status: string;
    description: string;
    coverImage: string;
    documentLink: string;
    price: number;
    size: string;
    documentTypeIds: number[];
    warehouseId: number;
    courseIds: number[],
}
interface DocumentType {
    documentTypeId: number;
    typeName: string;
}

interface Course {
    courseId: number; // Changed to courseId
    courseName: string;
}

interface DocumentTypeRes {
    code: number;
    message: string;
    data: {
        content: DocumentType[]
    };
}
interface CourseRes {
    code: number;
    message: string;
    data: {
        content: Course[];
    };
}

interface SelectedFile {
    file: File;
    type: 'pdf' | 'word';
}

const VisuallyHiddenInput = styled('input')({
    clip: 'rect(0 0 0 0)',
    clipPath: 'inset(50%)',
    height: 1,
    overflow: 'hidden',
    position: 'absolute',
    bottom: 0,
    left: 0,
    whiteSpace: 'nowrap',
    width: 1,
});

const AddBookPage: React.FC = () => {
    const [book, setBook] = useState<Book>({
        isbn: '',
        documentName: '',
        author: '',
        publisher: '',
        publishedDate: '',
        pageCount: 0,
        language: '',
        quantity: 0,
        availableCount: 0,
        status: 'AVAILABLE',
        description: '',
        coverImage: '',
        documentLink: '',
        price: 0,
        size: 'MEDIUM',
        documentTypeIds: [],
        warehouseId: 1,
        courseIds: [],
    });


    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [selectedPdfFile, setSelectedPdfFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<string | null>(null);
    const [documentTypes, setDocumentTypes] = useState<DocumentType[]>([]);
    const [selectedTags, setSelectedTags] = useState<number[]>([]);
    const [openAddTypeDialog, setOpenAddTypeDialog] = useState(false);
    const [openAddCourseDialog, setOpenAddCourseDialog] = useState(false);
    const [openImportDiolog, setOpenImportDiolog] = useState(false);
    const [newTypeName, setNewTypeName] = useState('');
    const [newCourseCode, setNewCourseCode] = useState('');
    const [newDescription, setNewDescription] = useState('');
    const [courses, setCourses] = useState<Course[]>([]);
    //const [selectedTypes, setSelectedTypes] = useState<number[]>([]);
    const [selectedCourses, setSelectedCourses] = useState<number[]>([]);
    const [openSnackbar, setOpenSnackbar] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');
    const [snackbarSeverity, setSnackbarSeverity] = useState<'success' | 'error'>('success');
    const [selectedFiles, setSelectedFiles] = useState<SelectedFile[]>([]);
    const [filesError, setFilesError] = useState<string>('');
    const [isAdding, setIsAdding] = useState(false);

    useEffect(() => {
        fetchDocumentTypes();
        fetchCourses();
    }, []);
    const showSnackbar = (message: string, severity: 'success' | 'error') => {
        setSnackbarMessage(message);
        setSnackbarSeverity(severity);
        setOpenSnackbar(true);

        setTimeout(() => {
            setOpenSnackbar(false);
        }, 3000);
    };
    const fetchCourses = async () => {
        try {
            const response = await apiService.get<CourseRes>('/api/v1/courses'); // Fetch courses
            setCourses(response.data.data.content || []);
        } catch (error) {
            console.log('Error fetching courses:', error);
            setCourses([]);
        }
    };

    const fetchDocumentTypes = async () => {
        try {
            const response = await apiService.get<DocumentTypeRes>('/api/v1/document-types');
            setDocumentTypes(response.data.data.content || []);
            console.log(response);
        } catch (error) {
            console.log('Error fetching document types:', error);
            setDocumentTypes([]);
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setBook({ ...book, [name]: value });
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            setSelectedFile(file);
            setPreview(URL.createObjectURL(file)); // Tạo URL để xem trước ảnh
        }
    };

    const handlePdfChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const pdfFile = e.target.files[0];
            setSelectedPdfFile(pdfFile); // Lưu PDF file vào state
        }
    };

    const handleFilesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const newFiles: SelectedFile[] = Array.from(e.target.files)
                .map(file => {
                    if (file.type === 'application/pdf') {
                        return { file, type: 'pdf' as const };
                    } else if (
                        file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
                        file.type === 'application/msword'
                    ) {
                        return { file, type: 'word' as const };
                    }
                    return null;
                })
                .filter((f): f is SelectedFile => f !== null);

            setFilesError('');
            setSelectedFiles(prev => [...prev, ...newFiles]);
        }
    };

    const removeFile = (index: number) => {
        setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    };

    const handleTagToggle = (tagId: number) => {
        if (selectedTags.includes(tagId)) {
            setSelectedTags(selectedTags.filter((id) => id !== tagId));
        } else {
            setSelectedTags([...selectedTags, tagId]);
        }
    };

    const handleCourseToggle = (courseId: number) => {
        if (selectedCourses.includes(courseId)) {
            setSelectedCourses(selectedCourses.filter((id) => id !== courseId));
        } else {
            setSelectedCourses([...selectedCourses, courseId]);
        }
    };

    const handleAddBook = async () => {
        try {
            setIsAdding(true);
            const formData = new FormData();

            formData.append('isbn', book.isbn);
            formData.append('documentName', book.documentName);
            formData.append('author', book.author);
            formData.append('publisher', book.publisher);
            formData.append('publishedDate', book.publishedDate);
            formData.append('pageCount', book.pageCount.toString());
            formData.append('language', book.language);
            formData.append('quantity', book.quantity.toString());
            formData.append('availableCount', book.availableCount.toString());
            formData.append('status', book.status);
            formData.append('description', book.description);
            formData.append('price', book.price.toString());
            formData.append('size', book.size);
            formData.append('warehouseId', book.warehouseId.toString());

            selectedTags.forEach((tagId) => formData.append('documentTypeIds', tagId.toString()));
            selectedCourses.forEach((courseId) => formData.append('courseIds', courseId.toString()));
            if (selectedFile) {
                formData.append('coverImage', selectedFile);
            }

            // Append all selected files
            selectedFiles.forEach((selectedFile) => {
                formData.append('files', selectedFile.file);
            });

            const response = await apiService.post('/api/v1/documents', formData);

            showSnackbar('Thêm sách thành công!', 'success');

            // Reset form
            setBook({
                isbn: '',
                documentName: '',
                author: '',
                publisher: '',
                publishedDate: '',
                pageCount: 0,
                language: '',
                quantity: 0,
                availableCount: 0,
                status: 'AVAILABLE',
                description: '',
                coverImage: '',
                documentLink: '',
                price: 0,
                size: 'MEDIUM',
                documentTypeIds: [],
                warehouseId: 2,
                courseIds: [],
            });
            setSelectedTags([]);
            setSelectedCourses([]);
            setSelectedFile(null);
            setSelectedFiles([]);
            setPreview(null);
        } catch (error) {
            showSnackbar('Thêm sách thất bại!', 'error');
        } finally {
            setIsAdding(false);
        }
    };


    const handleOpenAddTypeDialog = () => {
        setOpenAddTypeDialog(true);
    };

    const handleCloseAddTypeDialog = () => {
        setOpenAddTypeDialog(false);
        setNewTypeName('');
        setNewDescription('');
    };
    const handleCloseAddCourseDialog = () => {
        setOpenAddCourseDialog(false);
        setNewTypeName('');
        setNewDescription('');
        setNewCourseCode('');
    };

    const handleAddNewType = async () => {
        const payload = {
            typeName: newTypeName,
            description: newDescription,
        };
        try {
            const response = await apiService.post('/api/v1/document-types', payload);
            console.log('New type added successfully:', response);
            showSnackbar('Thêm loại sách thành công!', 'success');
            handleCloseAddTypeDialog();
            fetchDocumentTypes();
            setNewTypeName('');
            setNewCourseCode('');
            setNewDescription('');
        } catch (error) {
            console.log('Failed to add new type:', error);
            showSnackbar('Thêm loại sách thất bại!', 'error');
        }
    };
    const handleAddNewCourse = async () => {
        const payload = {
            courseCode: newCourseCode,
            courseName: newTypeName,
            description: newDescription,
        };
        try {
            const response = await apiService.post('/api/v1/courses', payload);
            console.log('New course added successfully:', response);
            showSnackbar('Thêm khóa học thành công!', 'success');
            handleCloseAddCourseDialog();
            fetchDocumentTypes();
            setNewTypeName('');
            setNewCourseCode('');
            setNewDescription('');
        } catch (error) {
            console.log('Failed to add new course:', error);
            showSnackbar('Thêm khóa học thất bại!', 'error');
        }
    };


    // const handleAddType = () => {
    //     alert("Add Type clicked!");
    // };

    const handleAddSubjectCode = () => {
        setOpenAddCourseDialog(true);
    };
    const handleOpenImportDialog = () => {
        setOpenImportDiolog(true);
    };

    // Handle closing the import dialog
    const handleCloseImportDialog = () => {
        setOpenImportDiolog(false);
        showSnackbar('Thêm khóa học thành công!', 'success');
    };



    return (
        <Box display="flex" bgcolor="#f5f6fa" minHeight="100vh">
            <Sidebar />
            <Box flex={1} display="flex" justifyContent="center" alignItems="flex-start" p={0}>
                <Card sx={{ width: '98%', minHeight: '90vh', borderRadius: 4, boxShadow: 4, m: 3, p: 4, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                    <CardContent sx={{ p: 4 }}>
                        <Typography variant="h4" align="center" fontWeight={700} gutterBottom color="primary.main">
                            Thêm Sách Mới
                        </Typography>
                        <Divider sx={{ mb: 3 }} />
                        <Grid container spacing={4}>
                            {/* Left Column - Image and PDF Upload */}
                            <Grid item xs={12} md={4}>
                                <Box display="flex" flexDirection="column" alignItems="center" gap={3}>
                                    <Box
                                        width={220}
                                        height={320}
                                        border="2px dashed #bdbdbd"
                                        borderRadius={3}
                                        display="flex"
                                        alignItems="center"
                                        justifyContent="center"
                                        sx={{ cursor: 'pointer', bgcolor: '#fafafa', '&:hover': { borderColor: 'primary.main' } }}
                                    >
                                        <input
                                            type="file"
                                            accept="image/*"
                                            style={{ display: 'none' }}
                                            id="upload-avatar"
                                            onChange={handleFileChange}
                                        />
                                        <label htmlFor="upload-avatar" style={{ width: '100%', height: '100%' }}>
                                            {preview ? (
                                                <Avatar
                                                    src={preview}
                                                    alt="Ảnh bìa"
                                                    sx={{ width: '100%', height: '100%', borderRadius: 2 }}
                                                />
                                            ) : (
                                                <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" height="100%">
                                                    <UploadFileIcon fontSize="large" color="action" />
                                                    <Typography variant="caption" color="text.secondary">Tải ảnh bìa</Typography>
                                                </Box>
                                            )}
                                        </label>
                                    </Box>
                                    <Typography variant="caption" color="text.secondary" align="center">
                                        *.jpeg, *.jpg, *.png (Tối đa 100 KB)
                                    </Typography>
                                    <Divider sx={{ width: '100%' }} />
                                    <Box textAlign="center" width="100%">
                                        <Button
                                            component="label"
                                            variant="outlined"
                                            color="secondary"
                                            startIcon={<DescriptionIcon />}
                                            fullWidth
                                            sx={{
                                                height: '56px',
                                                justifyContent: 'flex-start',
                                                textTransform: 'none',
                                                mb: 2,
                                                borderRadius: '10px'
                                            }}
                                        >
                                            Tải lên tài liệu (PDF, Word)
                                            <VisuallyHiddenInput
                                                type="file"
                                                accept=".pdf,.doc,.docx"
                                                multiple
                                                onChange={handleFilesChange}
                                            />
                                        </Button>
                                        {filesError && (
                                            <Typography color="error" variant="caption" sx={{ mt: 1 }}>
                                                {filesError}
                                            </Typography>
                                        )}
                                        {selectedFiles.length > 0 && (
                                            <List sx={{ 
                                                maxHeight: '200px', 
                                                overflowY: 'auto',
                                                bgcolor: '#f5f5f5',
                                                borderRadius: '10px',
                                                p: 1
                                            }}>
                                                {selectedFiles.map((selectedFile, index) => (
                                                    <ListItem
                                                        key={index}
                                                        sx={{
                                                            bgcolor: 'white',
                                                            mb: 1,
                                                            borderRadius: '8px',
                                                            '&:last-child': {
                                                                mb: 0
                                                            }
                                                        }}
                                                        secondaryAction={
                                                            <IconButton 
                                                                edge="end" 
                                                                onClick={() => removeFile(index)}
                                                                sx={{
                                                                    color: '#f44336',
                                                                    '&:hover': {
                                                                        bgcolor: 'rgba(244, 67, 54, 0.08)'
                                                                    }
                                                                }}
                                                            >
                                                                <CloseIcon />
                                                            </IconButton>
                                                        }
                                                    >
                                                        <ListItemIcon>
                                                            {selectedFile.type === 'pdf' ? (
                                                                <PictureAsPdfIcon color="error" />
                                                            ) : (
                                                                <ArticleIcon color="primary" />
                                                            )}
                                                        </ListItemIcon>
                                                        <ListItemText
                                                            primary={
                                                                <Typography
                                                                    variant="body2"
                                                                    sx={{
                                                                        fontWeight: 500,
                                                                        overflow: 'hidden',
                                                                        textOverflow: 'ellipsis',
                                                                        whiteSpace: 'nowrap',
                                                                        maxWidth: '200px'
                                                                    }}
                                                                >
                                                                    {selectedFile.file.name}
                                                                </Typography>
                                                            }
                                                            secondary={
                                                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                                    <Typography variant="caption" color="text.secondary">
                                                                        {(selectedFile.file.size / 1024 / 1024).toFixed(2)} MB
                                                                    </Typography>
                                                                    <Typography variant="caption" color="text.secondary">
                                                                        {selectedFile.type.toUpperCase()}
                                                                    </Typography>
                                                                </Box>
                                                            }
                                                        />
                                                    </ListItem>
                                                ))}
                                            </List>
                                        )}
                                    </Box>
                                </Box>
                            </Grid>
                            {/* Phải: Thông tin sách */}
                            <Grid item xs={12} md={8}>
                                <Grid container spacing={3}>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Mã ISBN" name="isbn" value={book.isbn} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Tên sách" name="documentName" value={book.documentName} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Tác giả" name="author" value={book.author} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Nhà xuất bản" name="publisher" value={book.publisher} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Ngày xuất bản" name="publishedDate" type="date" InputLabelProps={{ shrink: true }} value={book.publishedDate} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Số trang" name="pageCount" type="number" value={book.pageCount} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Ngôn ngữ" name="language" value={book.language} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Số lượng" name="quantity" type="number" value={book.quantity} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Số lượng có sẵn" name="availableCount" type="number" value={book.availableCount} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12} sm={6}>
                                        <TextField size="small" fullWidth label="Giá" name="price" type="number" value={book.price} onChange={handleChange} />
                                    </Grid>
                                    <Grid item xs={12}>
                                        <TextField size="small" fullWidth label="Mô tả" name="description" value={book.description} onChange={handleChange} multiline rows={2} />
                                    </Grid>
                                </Grid>
                            </Grid>
                        </Grid>
                        <Divider sx={{ my: 4 }} />
                        {/* Tags Selection */}
                        <Box mt={2}>
                            <Typography variant="h6" fontWeight={600} gutterBottom>Chọn loại sách</Typography>
                            <Box mt={1} display="flex" flexWrap="wrap" gap={2}>
                                {(Array.isArray(documentTypes) ? documentTypes : []).map((tag) => (
                                    <Chip
                                        key={tag.documentTypeId}
                                        label={tag.typeName}
                                        clickable
                                        color={selectedTags.includes(tag.documentTypeId) ? 'primary' : 'default'}
                                        onClick={() => handleTagToggle(tag.documentTypeId)}
                                        onDelete={selectedTags.includes(tag.documentTypeId) ? () => handleTagToggle(tag.documentTypeId) : undefined}
                                        sx={{ fontSize: 16, px: 2, py: 1 }}
                                    />
                                ))}
                            </Box>
                        </Box>
                        <Box mt={4}>
                            <Typography variant="h6" fontWeight={600} gutterBottom>
                                Chọn khóa học <Typography component="span" variant="caption" color="text.secondary">(Tùy chọn)</Typography>
                            </Typography>
                            <Box mt={1} display="flex" flexWrap="wrap" gap={2}>
                                {courses.map((course) => (
                                    <Chip
                                        key={course.courseId}
                                        label={course.courseName}
                                        clickable
                                        color={selectedCourses.includes(course.courseId) ? 'primary' : 'default'}
                                        onClick={() => handleCourseToggle(course.courseId)}
                                        onDelete={selectedCourses.includes(course.courseId) ? () => handleCourseToggle(course.courseId) : undefined}
                                        sx={{ fontSize: 16, px: 2, py: 1 }}
                                    />
                                ))}
                            </Box>
                        </Box>
                        <Box mt={5} textAlign="center">
                            <Button 
                                variant="contained" 
                                color="primary" 
                                size="medium" 
                                onClick={handleAddBook} 
                                sx={{ px: 4, py: 1.5, fontWeight: 700, fontSize: 16, borderRadius: '15px' }}
                                disabled={
                                    isAdding ||
                                    !book.documentName.trim() ||
                                    !book.author.trim() ||
                                    !book.publisher.trim() ||
                                    !book.publishedDate.trim() ||
                                    !book.pageCount ||
                                    !book.language.trim() ||
                                    !book.quantity ||
                                    !book.description.trim() ||
                                    selectedTags.length === 0
                                }
                                startIcon={isAdding ? <CircularProgress size={20} color="inherit" /> : null}
                            >
                                {isAdding ? 'Đang thêm sách...' : 'Thêm sách'}
                            </Button>
                        </Box>
                    </CardContent>
                </Card>
                <SpeedDial
                    ariaLabel="Thêm nhanh"
                    sx={{ position: 'fixed', bottom: 16, right: 16 }}
                    icon={<AddIcon />}
                >
                    <SpeedDialAction
                        icon={<LabelIcon />}
                        tooltipTitle="Thêm loại sách"
                        onClick={handleOpenAddTypeDialog}
                    />
                    <SpeedDialAction
                        icon={<SubjectIcon />}
                        tooltipTitle="Thêm khóa học"
                        onClick={handleAddSubjectCode}
                    />
                    <SpeedDialAction
                        icon={<PublishIcon />}
                        tooltipTitle="Nhập từ Excel"
                        onClick={handleOpenImportDialog}
                    />
                </SpeedDial>
                <Dialog open={openAddTypeDialog} onClose={handleCloseAddTypeDialog}>
                    <DialogTitle>Thêm loại sách mới</DialogTitle>
                    <DialogContent>
                        <TextField
                            autoFocus
                            margin="dense"
                            label="Tên loại sách"
                            type="text"
                            fullWidth
                            value={newTypeName}
                            onChange={(e) => setNewTypeName(e.target.value)}
                        />
                        <TextField
                            margin="dense"
                            label="Mô tả"
                            type="text"
                            fullWidth
                            multiline
                            rows={3}
                            value={newDescription}
                            onChange={(e) => setNewDescription(e.target.value)}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleCloseAddTypeDialog} color="primary">
                            Hủy
                        </Button>
                        <Button onClick={handleAddNewType} color="primary" variant="contained">
                            Thêm
                        </Button>
                    </DialogActions>
                </Dialog>
                <Dialog open={openAddCourseDialog} onClose={handleCloseAddCourseDialog}>
                    <DialogTitle>Thêm khóa học mới</DialogTitle>
                    <DialogContent>
                        <TextField
                            autoFocus
                            margin="dense"
                            label="Mã khóa học"
                            type="text"
                            fullWidth
                            value={newCourseCode}
                            onChange={(e) => setNewCourseCode(e.target.value)}
                        />
                        <TextField
                            autoFocus
                            margin="dense"
                            label="Tên khóa học"
                            type="text"
                            fullWidth
                            value={newTypeName}
                            onChange={(e) => setNewTypeName(e.target.value)}
                        />
                        <TextField
                            margin="dense"
                            label="Mô tả"
                            type="text"
                            fullWidth
                            multiline
                            rows={3}
                            value={newDescription}
                            onChange={(e) => setNewDescription(e.target.value)}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleCloseAddCourseDialog} color="primary">
                            Hủy
                        </Button>
                        <Button onClick={handleAddNewCourse} color="primary" variant="contained">
                            Thêm
                        </Button>
                    </DialogActions>
                </Dialog>
                <ImportExcelDialog
                    open={openImportDiolog}
                    onClose={handleCloseImportDialog}
                />
                <Snackbar
                    open={openSnackbar}
                    autoHideDuration={3000}
                    onClose={() => setOpenSnackbar(false)}
                    anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                >
                    <Alert onClose={() => setOpenSnackbar(false)} severity={snackbarSeverity} variant="filled">
                        {snackbarMessage}
                    </Alert>
                </Snackbar>
            </Box>
        </Box>
    );
};

export default AddBookPage;
