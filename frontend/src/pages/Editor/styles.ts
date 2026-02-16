import styled from "styled-components";


export const EditorContainer = styled.div`
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  background: #fff;
  overflow: hidden;
`;

export const TextareaWrapper = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
`;

export const StyledTextarea = styled.textarea`
  width: 100%;
  height: 100%;
  padding: 20px;
  font-size: 18px;
  font-family: monospace;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  caret-color: transparent;
  color: var(--color-text);
  position: relative;
  z-index: 1;
  overflow-y: auto;
  overflow-x: hidden;
  word-wrap: break-word;
  white-space: pre-wrap;
  
  &::placeholder {
    color: var(--color-gray-300);
  }
  
  &::-webkit-scrollbar {
    width: 8px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background: var(--color-gray-200);
    border-radius: 4px;
  }
  
  &::-webkit-scrollbar-thumb:hover {
    background: var(--color-gray-300);
  }
`;


