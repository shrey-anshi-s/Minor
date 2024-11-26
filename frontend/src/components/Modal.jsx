import React from 'react';
import './Modal.css'; // Import the CSS for styling

const Modal = ({ isOpen, onClose, children }) => {
    if (!isOpen) return null; // If the modal is not open, return null

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="close-button" onClick={onClose}>
                    &times; {/* Close button (X) */}
                </button>
                {children} {/* Render the content passed to the modal */}
            </div>
        </div>
    );
};

export default Modal;